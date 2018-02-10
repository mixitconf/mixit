package mixit.web.handler

import mixit.MixitProperties
import mixit.model.Language
import mixit.model.Link
import mixit.model.Role
import mixit.model.User
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.json
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.util.UriUtils
import reactor.core.publisher.toMono
import java.net.URI.create
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


@Component
class UserHandler(private val repository: UserRepository,
                  private val talkRepository: TalkRepository,
                  private val markdownConverter: MarkdownConverter,
                  private val properties: MixitProperties) {

    companion object {
        val speakerStarInHistory = listOf(
                "tastapod",
                "joel.spolsky",
                "pamelafox",
                "MattiSG",
                "bodil",
                "mojavelinux",
                "andrey.breslav",
                "kowen",
                "ppezziardi",
                "rising.linda")
        val speakerStarInCurrentEvent = listOf(
                "jhoeller@pivotal.io",
                "sharon@sharonsteed.co",
                "agilex",
                "laura.carvajal@gmail.com",
                "augerment@gmail.com",
                "dgageot",
                "romainguy@curious-creature.com",
                "graphicsgeek1@gmail.com",
                "sam@sambrannen.com")
    }


    fun findOneView(req: ServerRequest) =
            try {
                val idLegacy = req.pathVariable("login").toLong()
                repository.findByLegacyId(idLegacy).flatMap { findOneViewDetail(it, req) }
            } catch (e: NumberFormatException) {
                repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8")).flatMap { findOneViewDetail(it, req) }
            }

    private fun findOneViewDetail(user: User, req: ServerRequest) =
            talkRepository
                    .findBySpeakerId(listOf(user.login))
                    .collectList()
                    .flatMap { talks ->
                        talks.map { talk -> talk.toDto(req.language(), listOf(user)) }.toMono()
                        ok()
                                .render("user", mapOf(
                                        Pair("user", user.toDto(req.language(), markdownConverter)),
                                        Pair("talks", talks),
                                        Pair("baseUri", UriUtils.encode(properties.baseUri!!, StandardCharsets.UTF_8))
                                ))
                    }


    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun findStaff(req: ServerRequest) = ok().json().body(repository.findByRoles(listOf(Role.STAFF)))

    fun findOneStaff(req: ServerRequest) = ok().json().body(repository.findOneByRoles(req.pathVariable("login"), listOf(Role.STAFF, Role.STAFF_IN_PAUSE)))

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>()).flatMap {
        created(create("/api/user/${it.login}")).json().body(it.toMono())
    }
}

class SpeakerStarDto(
        val login: String,
        val key: String,
        val name: String
)

fun User.toSpeakerStarDto() = SpeakerStarDto(login, lastname.toLowerCase(), "$firstname $lastname")

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
        val logoWebpUrl: String? = null
)

fun User.toDto(language: Language, markdownConverter: MarkdownConverter) =
        UserDto(login,
                firstname,
                lastname,
                email,
                company, markdownConverter.toHTML(description[language] ?: ""),
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
