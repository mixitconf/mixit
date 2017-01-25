package mixit.controller

import mixit.model.Role
import mixit.model.User
import mixit.repository.UserRepository
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.net.URI
import java.net.URLDecoder


@Controller
class UserController(val repository: UserRepository) : RouterFunction<ServerResponse> {

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            (GET("/user/") or GET("/users/")) { findAllView() }
            GET("/user/{login}") { findOneView(req) }
            GET("/sponsor/{login}") { findOneSponsorView(req) }
        }
        accept(APPLICATION_JSON).apply {
            GET("/api/user/") { findAll() }
            POST("/api/user/") { create(req) }
            GET("/api/user/{login}") { findOne(req) }
            GET("/api/staff/") { findStaff() }
            GET("/api/staff/{login}") { findOneStaff(req) }
            GET("/api/speaker/") { findSpeakers() }
            GET("/api/speaker/{login}") { findOneSpeaker(req) }
            GET("/api/sponsor/") { findSponsors() }
            GET("/api/sponsor/{login}") { findOneSponsor(req) }
            GET("/api/{event}/speaker/") { findSpeakersByEvent(req) }
        }
    }

    fun findOneView(req: ServerRequest) = repository.findOne(req.pathVariable("login"))
            .then { u -> ok().render("user", mapOf(Pair("user", u))) }

    fun findOneSponsorView(req: ServerRequest) =
        repository.findOne(URLDecoder.decode(req.pathVariable("login"), "UTF-8"))
                .then { u -> ok().render("sponsor", mapOf(Pair("sponsor", u))) }

    fun findAllView() = repository.findAll()
            .collectList()
            .then { u -> ok().render("users",  mapOf(Pair("users", u))) }

    fun findOne(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOne(req.pathVariable("login"))))

    fun findAll() = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findAll()))

    fun findStaff() = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.STAFF)))

    fun findOneStaff(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.STAFF)))

    fun findSpeakers() = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.SPEAKER)))

    fun findSpeakersByEvent(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRoleAndEvent(Role.SPEAKER, req.pathVariable("event"))))

    fun findOneSpeaker(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.SPEAKER)))

    fun findSponsors() = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findByRole(Role.SPONSOR)))

    fun findOneSponsor(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.SPONSOR)))

    fun create(req: ServerRequest) = repository.save(req.bodyToMono<User>())
            .then { u -> created(URI.create("/api/user/${u.login}"))
                .contentType(APPLICATION_JSON_UTF8)
                .body(fromObject(u))
            }

}