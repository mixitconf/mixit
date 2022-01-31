package mixit.user.handler

import java.net.URI.create
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.stream.IntStream
import mixit.MixitProperties
import mixit.security.model.Cryptographer
import mixit.talk.handler.toDto
import mixit.talk.model.Language
import mixit.talk.repository.TalkRepository
import mixit.ticket.repository.TicketRepository
import mixit.user.model.Link
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.anonymize
import mixit.user.repository.UserRepository
import mixit.util.encodeToMd5
import mixit.util.extractFormData
import mixit.util.json
import mixit.util.language
import mixit.util.seeOther
import mixit.util.validator.EmailValidator
import mixit.util.validator.MarkdownValidator
import mixit.util.validator.MaxLengthValidator
import mixit.util.validator.UrlValidator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class UserHandler(
    private val repository: UserRepository,
    private val talkRepository: TalkRepository,
    private val ticketRepository: TicketRepository,
    private val cryptographer: Cryptographer,
    private val properties: MixitProperties,
    private val emailValidator: EmailValidator,
    private val urlValidator: UrlValidator,
    private val maxLengthValidator: MaxLengthValidator,
    private val markdownValidator: MarkdownValidator
) {

    enum class ViewMode { ViewMyProfile, ViewUser, EditProfile }

    fun speakerView(req: ServerRequest) = ok().render("speaker", mapOf(Pair("title", "speaker.title")))

    fun findOneView(req: ServerRequest) =
        try {
            val idLegacy = req.pathVariable("login").toLong()
            repository.findByLegacyId(idLegacy).flatMap { findOneViewDetail(req, it) }
        } catch (e: NumberFormatException) {
            repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8"))
                .flatMap { findOneViewDetail(req, it) }
        }

    fun findProfileView(req: ServerRequest) =
        req.session().flatMap {
            val currentUserEmail = it.getAttribute<String>("email")
            repository.findByNonEncryptedEmail(currentUserEmail!!).flatMap {
                findOneViewDetail(
                    req, it,
                    ViewMode.ViewMyProfile
                )
            }
        }

    fun editProfileView(req: ServerRequest) =
        req.session().flatMap {
            val currentUserEmail = it.getAttribute<String>("email")
            repository.findByNonEncryptedEmail(currentUserEmail!!).flatMap {
                findOneViewDetail(
                    req, it,
                    ViewMode.EditProfile
                )
            }
        }

    private fun findOneViewDetail(
        req: ServerRequest,
        user: User,
        viewMode: ViewMode = ViewMode.ViewUser,
        errors: Map<String, String> = emptyMap()
    ): Mono<ServerResponse> =
        if (viewMode == ViewMode.EditProfile) {
            ok().render(
                "user-edit",
                mapOf(
                    Pair("user", user.toDto(req.language())),
                    Pair("usermail", cryptographer.decrypt(user.email)),
                    Pair("description-fr", user.description[Language.FRENCH]),
                    Pair("description-en", user.description[Language.ENGLISH]),
                    Pair("userlinks", user.toLinkDtos()),
                    Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                    Pair("errors", errors),
                    Pair("hasErrors", errors.isNotEmpty())
                )
            )
        } else if (viewMode == ViewMode.ViewMyProfile) {
            ticketRepository
                .findByEmail(cryptographer.decrypt(user.email)!!)
                .flatMap {
                    ok().render(
                        "user",
                        mapOf(
                            Pair("user", user.toDto(req.language())),
                            Pair("hasLotteryTicket", true),
                            Pair("canUpdateProfile", true),
                            Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8))
                        )
                    )
                }
                .switchIfEmpty(
                    ok().render(
                        "user",
                        mapOf(
                            Pair("user", user.toDto(req.language())),
                            Pair("canUpdateProfile", true),
                            Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8))
                        )
                    )
                )
        } else {
            talkRepository
                .findBySpeakerId(listOf(user.login))
                .collectList()
                .flatMap { talks ->
                    val talkDtos = talks.map { talk -> talk.toDto(req.language(), listOf(user)) }
                    ok().render(
                        "user",
                        mapOf(
                            Pair("user", user.toDto(req.language())),
                            Pair("talks", talkDtos),
                            Pair("hasTalks", talkDtos.isNotEmpty()),
                            Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8))
                        )
                    )
                }
        }

    fun saveProfile(req: ServerRequest): Mono<ServerResponse> =
        req.session().flatMap { session ->
            val currentUserEmail = session.getAttribute<String>("email")
            req.extractFormData().flatMap { formData ->

                // In his profile screen a user can't change all the data. In the first step we load the user
                repository.findByNonEncryptedEmail(currentUserEmail!!).flatMap { user ->
                    val requiredFields = listOf("firstname", "lastname", "email", "description-fr", "description-en")
                    val requiredErrors: Map<String, String> = requiredFields
                        .mapNotNull { key ->
                            if (formData[key] == null) key to "user.form.error.$key.required" else null
                        }
                        .toMap()
                    if (requiredErrors.isNotEmpty()) {
                        return@flatMap findOneViewDetail(req, user, ViewMode.EditProfile, errors = requiredErrors)
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
                        links = extractLinks(formData)
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
                    if (updatedUser.photoUrl !=null && !urlValidator.isValid(updatedUser.photoUrl)) {
                        errors["photoUrl"] = "user.form.error.photourl"
                    }
                    user.links.forEachIndexed { index, link ->
                        if (!maxLengthValidator.isValid(link.name, 30)) {
                            errors["link${index + 1}Name"] = "user.form.error.link${index + 1}.name"
                        }
                        if (!urlValidator.isValid(link.url)) {
                            errors["link${index + 1}Url"] = "user.form.error.link${index + 1}.url"
                        }
                    }
                    if (errors.isEmpty()) {
                        // If everything is Ok we save the user
                        repository.save(updatedUser).then(seeOther("${properties.baseUri}/me"))
                    } else {
                        findOneViewDetail(req, updatedUser, ViewMode.EditProfile, errors = errors)
                    }
                }
            }
        }

    private fun extractLinks(formData: Map<String, String?>): List<Link> =
        IntStream.range(0, 5)
            .toArray()
            .asList()
            .mapIndexed { index, _ -> Pair(formData["link${index}Name"], formData["link${index}Url"]) }
            .filter { !it.first.isNullOrBlank() && !it.second.isNullOrBlank() }
            .map { Link(it.first!!, it.second!!) }

    fun findOne(req: ServerRequest) =
        ok().json().body(repository.findOne(req.pathVariable("login")).map { it.anonymize() })

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll().map { it.anonymize() })

    fun findStaff(req: ServerRequest) =
        ok().json().body(repository.findByRoles(listOf(Role.STAFF)).map { it.anonymize() })

    fun findOneStaff(req: ServerRequest) = ok().json().body(
        repository.findOneByRoles(req.pathVariable("login"), listOf(Role.STAFF, Role.STAFF_IN_PAUSE))
            .map { it.anonymize() })

    fun findSpeakerByEventId(req: ServerRequest) =
        ok().json().body(
            talkRepository.findByEvent(req.pathVariable("year"))
                .flatMap { repository.findMany(it.speakerIds).map { it.anonymize() } }.distinct()
        )

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>()).flatMap {
        created(create("/api/user/${it.login}")).json().body(it.toMono())
    }

    fun check(req: ServerRequest) = ok().json().body(
        repository.findByNonEncryptedEmail(req.pathVariable("email"))
            .filter { it.token == req.headers().header("token").get(0) }
            .map { it.toDto(req.language()) }
    )
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
