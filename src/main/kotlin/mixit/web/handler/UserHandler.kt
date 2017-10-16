package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.net.URI.*
import java.net.URLDecoder


@Component
class UserHandler(private val repository: UserRepository,
                  private val markdownConverter: MarkdownConverter) {

    fun findOneView(req: ServerRequest) =
            try {
                val idLegacy = req.pathVariable("login").toLong()
                repository.findByLegacyId(idLegacy).flatMap {
                    ok().render("user", mapOf(Pair("user", it.toDto(req.language(), markdownConverter))))
                }
            } catch (e:NumberFormatException) {
                repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8")).flatMap {
                    ok().render("user", mapOf(Pair("user", it.toDto(req.language(), markdownConverter))))
                }
            }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun findStaff(req: ServerRequest) = ok().json().body(repository.findByRoles(listOf(Role.STAFF)))

    fun findOneStaff(req: ServerRequest) = ok().json().body(repository.findOneByRoles(req.pathVariable("login"), listOf(Role.STAFF, Role.STAFF_IN_PAUSE)))

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>()).flatMap {
        created(create("/api/user/${it.login}")).json().body(it.toMono())
    }
}

class UserDto(
        val login: String,
        val firstname: String,
        val lastname: String,
        var email: String,
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
        UserDto(login, firstname, lastname, email ?: "", company, markdownConverter.toHTML(description[language] ?: ""),
                emailHash, photoUrl, role, links, logoType(photoUrl), logoWebpUrl(photoUrl))

private fun logoWebpUrl(url: String?) =
        when {
            url == null -> null
            url.endsWith("png") -> url.replace("png", "webp")
            url.endsWith("jpg") -> url.replace("jpg", "webp")
            else -> null
        }

private fun logoType(url: String?) =
        when {
            url == null -> null
            url.endsWith("svg") -> "image/svg+xml"
            url.endsWith("png") -> "image/png"
            url.endsWith("jpg") -> "image/jpeg"
            else -> null
        }
