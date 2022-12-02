package mixit.user.handler

import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitProperties
import mixit.routes.MustacheI18n.ERRORS
import mixit.routes.MustacheI18n.HAS_ERRORS
import mixit.routes.MustacheI18n.HAS_TALKS
import mixit.routes.MustacheI18n.TALKS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheI18n.USER
import mixit.routes.MustacheTemplate
import mixit.routes.MustacheTemplate.Speaker
import mixit.routes.MustacheTemplate.UserEdit
import mixit.security.model.Cryptographer
import mixit.talk.model.Language
import mixit.talk.model.TalkService
import mixit.ticket.model.TicketService
import mixit.ticket.repository.LotteryRepository
import mixit.user.handler.dto.toDto
import mixit.user.handler.dto.toLinkDtos
import mixit.user.model.Link
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.repository.UserRepository
import mixit.util.currentNonEncryptedUserEmail
import mixit.util.decode
import mixit.util.encodeToMd5
import mixit.util.extractFormData
import mixit.util.language
import mixit.util.seeOther
import mixit.util.validator.EmailValidator
import mixit.util.validator.MarkdownValidator
import mixit.util.validator.MaxLengthValidator
import mixit.util.validator.UrlValidator
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
import java.util.stream.IntStream

@Component
class UserHandler(
    private val repository: UserRepository,
    private val userService: UserService,
    private val service: TalkService,
    private val lotteryRepository: LotteryRepository,
    private val ticketService: TicketService,
    private val cryptographer: Cryptographer,
    private val properties: MixitProperties,
    private val emailValidator: EmailValidator,
    private val maxLengthValidator: MaxLengthValidator,
    private val markdownValidator: MarkdownValidator
) {

    enum class ViewMode { ViewMyProfile, ViewUser, EditProfile }

    suspend fun speakerView(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(Speaker.template, mapOf(Pair(TITLE, Speaker.title)))

    suspend fun findOneView(req: ServerRequest): ServerResponse {
        val login = req.decode("login")!!
        val user =
            repository.findOneOrNull(login) ?: repository.findByLegacyId(login.toLong()) ?: throw NotFoundException()
        return findOneViewDetail(req, user)
    }

    suspend fun findProfileView(req: ServerRequest): ServerResponse {
        val user = repository.findByNonEncryptedEmail(req.currentNonEncryptedUserEmail()) ?: throw NotFoundException()
        return findOneViewDetail(req, user, ViewMode.ViewMyProfile)
    }

    suspend fun editProfileView(req: ServerRequest): ServerResponse {
        val user = repository.findByNonEncryptedEmail(req.currentNonEncryptedUserEmail()) ?: throw NotFoundException()
        return findOneViewDetail(req, user, ViewMode.EditProfile)
    }

    private suspend fun findOneViewDetail(
        req: ServerRequest,
        user: User,
        viewMode: ViewMode = ViewMode.ViewUser,
        errors: Map<String, String> = emptyMap()
    ): ServerResponse =
        when (viewMode) {
            ViewMode.ViewMyProfile -> {
                val params = mapOf(
                    USER to user.toDto(req.language()),
                    ERRORS to errors,
                    HAS_ERRORS to errors.isNotEmpty(),
                    "usermail" to cryptographer.decrypt(user.email),
                    "description-fr" to user.description[Language.FRENCH],
                    "description-en" to user.description[Language.ENGLISH],
                    "userlinks" to user.toLinkDtos()
                )
                ok().renderAndAwait(MustacheTemplate.User.template, params)
            }

            ViewMode.ViewUser -> {
                val isSpeaker = service
                    .findBySpeakerId(listOf(user.login), "null")
                    .any { it.event == CURRENT_EVENT }

                val attendeeTicket = ticketService.findByEmail(cryptographer.decrypt(user.email)!!)
                val lottery = lotteryRepository.findByEncryptedEmail(user.email ?: "unknown")
                val params = mapOf(
                    USER to user.toDto(req.language()),
                    "lotteryTicket" to lottery,
                    "attendeeTicket" to attendeeTicket,
                    "viewMyProfile" to false,
                    "isSpeaker" to isSpeaker,
                    "canUpdateProfile" to true,
                )
                ok().renderAndAwait(MustacheTemplate.User.template, params)
            }

            ViewMode.EditProfile -> {
                val talks = service.findBySpeakerId(listOf(user.login), "all")
                val params = mapOf(
                    USER to user.toDto(req.language()),
                    TALKS to talks.map { it.toDto(req.language()) },
                    HAS_TALKS to talks.isNotEmpty()
                )
                ok().renderAndAwait(UserEdit.template, params)
            }
        }

    suspend fun saveProfile(req: ServerRequest): ServerResponse {
        val user = repository.findByNonEncryptedEmail(req.currentNonEncryptedUserEmail()) ?: throw NotFoundException()
        val formData = req.extractFormData()

        // In his profile screen a user can't change all the data. In the first step we load the user
        val requiredFields = listOf("firstname", "lastname", "email", "description-fr", "description-en")
        val requiredErrors: Map<String, String> = requiredFields
            .mapNotNull { key ->
                if (formData[key] == null) key to "user.form.error.$key.required" else null
            }
            .toMap()

        if (requiredErrors.isNotEmpty()) {
            return findOneViewDetail(req, user, ViewMode.EditProfile, errors = requiredErrors)
        }

        val updatedUser = user.copy(
            firstname = formData["firstname"]!!,
            lastname = formData["lastname"]!!,
            email = cryptographer.encrypt(formData["email"]!!),
            company = formData["company"],
            description = mapOf(
                Pair(Language.FRENCH, markdownValidator.sanitize(formData["description-fr"]!!)),
                Pair(Language.ENGLISH, markdownValidator.sanitize(formData["description-en"]!!))
            ),
            emailHash = if (formData["photoUrl"] == null) formData["email"]!!.encodeToMd5() else null,
            photoUrl = formData["photoUrl"],
            links = extractLinks(formData),
            newsletterSubscriber = formData["newsletterSubscriber"]?.toBoolean() ?: false
        )

        val errors = mutableMapOf<String, String>()

        // We want to control data to not save invalid things in our database
        if (!maxLengthValidator.isValid(updatedUser.firstname, 30)) {
            errors["firstname"] = "user.form.error.firstname.size"
        }
        if (!maxLengthValidator.isValid(updatedUser.lastname, 30)) {
            errors["lastname"] = "user.form.error.lastname.size"
        }
        if (updatedUser.company != null && !maxLengthValidator.isValid(updatedUser.company, 60)) {
            errors["company"] = "user.form.error.company.size"
        }
        if (!emailValidator.isValid(formData["email"]!!)) {
            errors["email"] = "user.form.error.email"
        }
        if (!markdownValidator.isValid(updatedUser.description[Language.FRENCH])) {
            errors["description-fr"] = "user.form.error.description.fr"
        }
        if (!markdownValidator.isValid(updatedUser.description[Language.ENGLISH])) {
            errors["description-en"] = "user.form.error.description.en"
        }
        if (updatedUser.photoUrl != null && !UrlValidator.isValid(updatedUser.photoUrl)) {
            errors["photoUrl"] = "user.form.error.photourl"
        }

        updatedUser.links.forEachIndexed { index, link ->
            if (!maxLengthValidator.isValid(link.name, 30)) {
                errors["link${index + 1}Name"] = "user.form.error.link${index + 1}.name"
            }
            if (!UrlValidator.isValid(link.url)) {
                errors["link${index + 1}Url"] = "user.form.error.link${index + 1}.url"
            }
        }
        if (errors.isEmpty()) {
            // If everything is Ok we save the user
            userService.save(updatedUser).awaitSingleOrNull()
            return seeOther("${properties.baseUri}/me")
        }
        return findOneViewDetail(req, updatedUser, ViewMode.EditProfile, errors = errors)
    }

    private fun extractLinks(formData: Map<String, String?>): List<Link> =
        List(IntStream.range(0, 5).toArray().asList().size) { index ->
            Pair(formData["link${index}Name"], formData["link${index}Url"])
        }
            .filter { !it.first.isNullOrBlank() && !it.second.isNullOrBlank() }
            .map { Link(it.first!!, it.second!!) }
}

fun logoWebpUrl(url: String?) =
    when {
        url == null -> null
        url.endsWith("png") -> url.replace("png", "webp")
        url.endsWith("jpg") -> url.replace("jpg", "webp")
        else -> null
    }

fun logoType(url: String?) =
    when {
        url == null -> null
        url.endsWith("svg") -> "image/svg+xml"
        url.endsWith("png") -> "image/png"
        url.endsWith("jpg") -> "image/jpeg"
        url.endsWith("gif") -> "image/gif"
        else -> null
    }
