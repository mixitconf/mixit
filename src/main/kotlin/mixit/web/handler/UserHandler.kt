package mixit.web.handler

import mixit.model.*
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.toMono
import java.net.URI.*
import java.net.URLDecoder


@Component
class UserHandler(val repository: UserRepository,
                  val markdownConverter: MarkdownConverter) {

    fun findOneView(req: ServerRequest) =
            try {
                val idLegacy = req.pathVariable("login").toLong()
                repository.findByLegacyId(idLegacy).then { u ->
                    ok().render("user", mapOf(Pair("user", u.toDto(req.language(), markdownConverter))))
                }
            } catch (e:NumberFormatException) {
                repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8")).then { u ->
                    ok().render("user", mapOf(Pair("user", u.toDto(req.language(), markdownConverter))))
                }
            }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun findStaff(req: ServerRequest) = ok().json().body(repository.findByRole(Role.STAFF))

    fun findOneStaff(req: ServerRequest) = ok().json().body(repository.findOneByRole(req.pathVariable("login"), Role.STAFF))

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>()).then { u ->
        created(create("/api/user/${u.login}")).json().body(u.toMono())
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
        val role: Role,
        var links: List<Link>,
        val logoType: String?,
        val logoWebpUrl: String? = null
)

fun User.toDto(language: Language, markdownConverter: MarkdownConverter) =
        UserDto(login, firstname, lastname, email ?: "", company, markdownConverter.toHTML(description[language] ?: ""),
                emailHash, role, links, logoType(emailHash), logoWebpUrl(emailHash))

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
