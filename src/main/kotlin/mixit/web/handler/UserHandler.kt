package mixit.web.handler

import mixit.MixitProperties
import mixit.model.Language
import mixit.model.Link
import mixit.model.Role
import mixit.model.User
import mixit.repository.TalkRepository
import mixit.repository.TicketRepository
import mixit.repository.UserRepository
import mixit.util.*
import mixit.util.validator.EmailValidator
import mixit.util.validator.MarkdownValidator
import mixit.util.validator.MaxLengthValidator
import mixit.util.validator.UrlValidator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.net.URI.create
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.stream.IntStream


@Component
class UserHandler(private val repository: UserRepository,
                  private val talkRepository: TalkRepository,
                  private val ticketRepository: TicketRepository,
                  private val markdownConverter: MarkdownConverter,
                  private val cryptographer: Cryptographer,
                  private val properties: MixitProperties,
                  private val emailValidator: EmailValidator,
                  private val urlValidator: UrlValidator,
                  private val maxLengthValidator: MaxLengthValidator,
                  private val markdownValidator: MarkdownValidator) {

    companion object {
        val speakerStarInHistory = listOf(
                "tastapod",
                "joel.spolsky",
                "pamelafox",
                "MattiSG",
                "bodil",
                "mojavelinux",
                "andrey.breslav",
                //"kowen",
                "ppezziardi",
                "rising.linda",
                "jhoeller",
                "sharonsteed",
                "allan.rennebo",
                "agilex",
                "laura.carvajal",
                "augerment",
                "dgageot",
                "romainguy",
                "graphicsgeek1",
                "sambrannen")
        val speakerStarInCurrentEvent = listOf(
                "bodil",
                "andre",
                "mary",
                "Woody.Zuill",
                "james.carlson",
                "egorcenski",
                "ojuncu",
                "hsablonniere",
                "nitot")
    }

    enum class ViewMode { ViewMyProfile, ViewUser, EditProfile }

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
                repository.findByEmail(currentUserEmail!!).flatMap { findOneViewDetail(req, it, ViewMode.ViewMyProfile) }
            }

    fun editProfileView(req: ServerRequest) =
            req.session().flatMap {
                val currentUserEmail = it.getAttribute<String>("email")
                repository.findByEmail(currentUserEmail!!).flatMap { findOneViewDetail(req, it, ViewMode.EditProfile) }
            }

    private fun findOneViewDetail(req: ServerRequest,
                                  user: User, viewMode: ViewMode = ViewMode.ViewUser,
                                  errors: Map<String, String> = emptyMap()): Mono<ServerResponse> =
            if (viewMode == ViewMode.EditProfile) {
                ok().render("user-edit", mapOf(
                        Pair("user", user.toDto(req.language(), markdownConverter)),
                        Pair("usermail", cryptographer.decrypt(user.email)),
                        Pair("description-fr", user.description[Language.FRENCH]),
                        Pair("description-en", user.description[Language.ENGLISH]),
                        Pair("userlinks", user.toLinkDtos()),
                        Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                        Pair("errors", errors),
                        Pair("hasErrors", errors.isNotEmpty())
                ))
            } else if (viewMode == ViewMode.ViewMyProfile) {
                ticketRepository
                        .findByEmail(cryptographer.decrypt(user.email)!!)
                        .flatMap {
                            ok().render("user", mapOf(
                                    Pair("user", user.toDto(req.language(), markdownConverter)),
                                    Pair("hasLotteryTicket", true),
                                    Pair("canUpdateProfile", true),
                                    Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8))
                            ))
                        }
                        .switchIfEmpty(
                                ok().render("user", mapOf(
                                        Pair("user", user.toDto(req.language(), markdownConverter)),
                                        Pair("canUpdateProfile", true),
                                        Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8))
                                        ))
                        )
            } else {
                talkRepository
                        .findBySpeakerId(listOf(user.login))
                        .collectList()
                        .flatMap { talks ->
                            val talkDtos = talks.map { talk -> talk.toDto(req.language(), listOf(user)) }
                            ok().render("user", mapOf(
                                    Pair("user", user.toDto(req.language(), markdownConverter)),
                                    Pair("talks", talkDtos),
                                    Pair("hasTalks", talkDtos.isNotEmpty()),
                                    Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8))
                            ))

                        }
            }


    fun saveProfile(req: ServerRequest): Mono<ServerResponse> = req.session().flatMap {
        val currentUserEmail = it.getAttribute<String>("email")
        req.body(BodyExtractors.toFormData()).flatMap {

            val formData = it.toSingleValueMap()

            // In his profile screen a user can't change all the data. In the first step we load the user
            repository.findByEmail(currentUserEmail!!).flatMap {

                val errors = mutableMapOf<String, String>()

                // Null check
                if (formData["firstname"].isNullOrBlank()) {
                    errors.put("firstname", "user.form.error.firstname.required")
                }
                if (formData["lastname"].isNullOrBlank()) {
                    errors.put("lastname", "user.form.error.lastname.required")
                }
                if (formData["email"].isNullOrBlank()) {
                    errors.put("email", "user.form.error.email.required")
                }
                if (formData["description-fr"].isNullOrBlank()) {
                    errors.put("description-fr", "user.form.error.description.fr.required")
                }
                if (formData["description-en"].isNullOrBlank()) {
                    errors.put("description-en", "user.form.error.description.en.required")
                }

                if (errors.isNotEmpty()) {
                    findOneViewDetail(req, it, ViewMode.EditProfile, errors = errors)
                }

                val user = User(
                        it.login,
                        formData["firstname"]!!,
                        formData["lastname"]!!,
                        cryptographer.encrypt(formData["email"]!!),
                        if (formData["company"] == "") null else formData["company"],
                        mapOf(
                                Pair(Language.FRENCH, markdownValidator.sanitize(formData["description-fr"]!!)),
                                Pair(Language.ENGLISH, markdownValidator.sanitize(formData["description-en"]!!))),
                        if (formData["photoUrl"].isNullOrBlank()) formData["email"]!!.encodeToMd5() else null,
                        if (formData["photoUrl"] == "") null else formData["photoUrl"],
                        it.role,
                        extractLinks(formData),
                        it.legacyId,
                        it.tokenExpiration,
                        it.token
                )


                // We want to control data to not save invalid things in our database
                if (!maxLengthValidator.isValid(user.firstname, 30)) {
                    errors.put("firstname", "user.form.error.firstname.size")
                }
                if (!maxLengthValidator.isValid(user.lastname, 30)) {
                    errors.put("lastname", "user.form.error.lastname.size")
                }
                if (user.company != null && !maxLengthValidator.isValid(user.company, 60)) {
                    errors.put("company", "user.form.error.company.size")
                }
                if (!emailValidator.isValid(formData["email"]!!)) {
                    errors.put("email", "user.form.error.email")
                }
                if (!markdownValidator.isValid(user.description.get(Language.FRENCH))) {
                    errors.put("description-fr", "user.form.error.description.fr")
                }
                if (!markdownValidator.isValid(user.description.get(Language.ENGLISH))) {
                    errors.put("description-en", "user.form.error.description.en")
                }
                if (!urlValidator.isValid(user.photoUrl)) {
                    errors.put("photoUrl", "user.form.error.photourl")
                }
                user.links.forEachIndexed { index, link ->
                    if (!maxLengthValidator.isValid(link.name, 30)) {
                        errors.put("link${index + 1}Name", "user.form.error.link${index + 1}.name")
                    }
                    if (!urlValidator.isValid(link.url)) {
                        errors.put("link${index + 1}Url", "user.form.error.link${index + 1}.url")
                    }
                }

                if (errors.isEmpty()) {
                    // If everything is Ok we save the user
                    repository.save(user).then(seeOther("${properties.baseUri}/me"))
                } else {
                    findOneViewDetail(req, user, ViewMode.EditProfile, errors = errors)
                }
            }
        }
    }

    private fun extractLinks(formData: Map<String, String>): List<Link> =
            IntStream.range(0, 5)
                    .toArray()
                    .asList()
                    .mapIndexed { index, _ -> Pair(formData["link${index}Name"], formData["link${index}Url"]) }
                    .filter { !it.first.isNullOrBlank() && !it.second.isNullOrBlank() }
                    .map { Link(it.first!!, it.second!!) }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun findStaff(req: ServerRequest) = ok().json().body(repository.findByRoles(listOf(Role.STAFF)))

    fun findOneStaff(req: ServerRequest) = ok().json().body(repository.findOneByRoles(req.pathVariable("login"), listOf(Role.STAFF, Role.STAFF_IN_PAUSE)))

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>()).flatMap {
        created(create("/api/user/${it.login}")).json().body(it.toMono())
    }

}

class LinkDto(
        val name: String,
        val url: String,
        val index: String)

fun Link.toLinkDto(index: Int) = LinkDto(name, url, "link${index + 1}")

fun User.toLinkDtos(): Map<String, List<LinkDto>> =
        if (links.size > 4) {
            links.mapIndexed { index, link -> link.toLinkDto(index) }.groupBy { it.index }
        } else {
            val existingLinks = links.size
            val userLinks = links.mapIndexed { index, link -> link.toLinkDto(index) }.toMutableList()
            IntStream.range(0, 5 - existingLinks).forEach { userLinks.add(LinkDto("", "", "link${existingLinks + it + 1}")) }
            userLinks.groupBy { it.index }
        }

class SpeakerStarDto(
        val login: String,
        val key: String,
        val name: String
)

fun User.toSpeakerStarDto() = SpeakerStarDto(login, lastname.toLowerCase().replace("è", "e"), "$firstname $lastname")

class UserDto(
        val login: String,
        val firstname: String,
        val lastname: String,
        var email: String? = null,
        var company: String? = null,
        var description: String,
        var emailHash: String? = null,
        var photoUrl: String? = null,
        val role: Role,
        var links: List<Link>,
        val logoType: String?,
        val logoWebpUrl: String? = null,
        val isAbsoluteLogo: Boolean = if (photoUrl == null) false else photoUrl.startsWith("http")
)

fun User.toDto(language: Language, markdownConverter: MarkdownConverter, searchTerms: List<String> = emptyList()) =
        UserDto(login,
                firstname.markFoundOccurrences(searchTerms),
                lastname.markFoundOccurrences(searchTerms),
                email,
                company,
                markdownConverter.toHTML(description[language] ?: "").markFoundOccurrences(searchTerms),
                emailHash,
                photoUrl,
                role,
                links,
                logoType(photoUrl),
                logoWebpUrl(photoUrl))

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
