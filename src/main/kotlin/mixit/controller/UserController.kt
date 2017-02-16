package mixit.controller

import mixit.model.Role
import mixit.model.User
import mixit.repository.UserRepository
import mixit.support.LazyRouterFunction
import mixit.support.MarkdownConverter
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import java.net.URI
import java.net.URLDecoder
import java.util.*


@Controller
class UserController(val repository: UserRepository, val markdownConverter: MarkdownConverter) : LazyRouterFunction() {

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: Routes.() -> Unit = {
        accept(TEXT_HTML).apply {
            (GET("/user/") or GET("/users/")) { findAllView() }
            (GET("/user/{login}") or GET("/speaker/{login}") or GET("/member/{login}") or GET("/member/member/{login}") or GET("/sponsor/{login}") or GET("/member/sponsor/{login}")) { findOneView(it) }
            (GET("/about/") or GET("/about")) { findAboutView() }
        }
        accept(APPLICATION_JSON).apply {
            GET("/api/user/", this@UserController::findAll)
            POST("/api/user/", this@UserController::create)
            GET("/api/user/{login}", this@UserController::findOne)
            GET("/api/staff/", this@UserController::findStaff)
            GET("/api/staff/{login}", this@UserController::findOneStaff)
            GET("/api/speaker/", this@UserController::findSpeakers)
            GET("/api/speaker/{login}", this@UserController::findOneSpeaker)
            GET("/api/sponsor/", this@UserController::findSponsors)
            GET("/api/sponsor/{login}", this@UserController::findOneSponsor)
            GET("/api/{event}/speaker/", this@UserController::findSpeakersByEvent)
        }
    }

    fun findOneView(req: ServerRequest) =
            try{
                val idLegacy = req.pathVariable("login").toLong()
                repository.findByLegacyId(idLegacy)
                        .then { u -> ok().render("user", mapOf(Pair("user", prepareForHtmlDisplay(u)))) }

            }
            catch (e:NumberFormatException){
                repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8"))
                        .then { u -> ok().render("user", mapOf(Pair("user", prepareForHtmlDisplay(u)))) }
            }

    fun findOneSponsorView(req: ServerRequest) =
        try{
            val idLegacy = req.pathVariable("login").toLong()
            repository.findByLegacyId(idLegacy)
                    .then { u -> ok().render("sponsor", mapOf(Pair("sponsor", u))) }

        }
        catch (e:NumberFormatException){
            repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8"))
                    .then { u -> ok().render("sponsor", mapOf(Pair("sponsor", u))) }
        }

    fun findAllView() = repository.findAll()
            .collectList()
            .then { u -> ok().render("users",  mapOf(Pair("users", u))) }

    fun findOne(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOne(req.pathVariable("login"))))

    fun findAll(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findAll()))

    fun findStaff(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.STAFF)))

    fun findOneStaff(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.STAFF)))

    fun findSpeakers(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.SPEAKER)))

    fun findSpeakersByEvent(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRoleAndEvent(Role.SPEAKER, req.pathVariable("event"))))

    fun findOneSpeaker(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.SPEAKER)))

    fun findSponsors(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.SPONSOR)))

    fun findOneSponsor(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.SPONSOR)))

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>())
            .then { u -> created(URI.create("/api/user/${u.login}"))
                .contentType(APPLICATION_JSON_UTF8)
                .body(fromObject(u))
            }

    fun findAboutView() = repository.findByRole(Role.STAFF)
            .collectList()
            .then { u ->
                val users = u.map { prepareForHtmlDisplay(it) }
                Collections.shuffle(users);
                ok().render("about",  mapOf(Pair("staff", users)))
            }

    fun prepareForHtmlDisplay(user :User): User {
        user.shortDescription = markdownConverter.toHTML(user.shortDescription)
        return user
    }
}