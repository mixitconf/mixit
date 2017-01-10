package mixit.controller

import mixit.model.Role
import mixit.model.User
import mixit.repository.UserRepository
import mixit.support.invoke
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.BodyInsertersExtension.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerRequestExtension.bodyToMono
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.net.URI

class UserController(val repository: UserRepository) : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) =
            "/" {
                "user" {
                    "/" {                GET { findAllView() } }
                    "/{login}" {         GET { findViewById() } }
                }
                "api/user" {
                    "/" {
                                         POST { create() }
                                         GET  { findAll() }
                    }
                    "/{login}" {         GET { findOne() } }
                }
                "api/staff" {
                    "/" {                GET { findStaff() } }
                    "/{login}" {         GET { findOneStaff() } }
                }
                "api/speaker" {
                    "/" {                GET { findSpeakers() } }
                    "/{login}" {         GET { findOneSpeaker() } }
                }
                "api/sponsor" {
                    "/" {                GET { findSponsors() } }
                    "/{login}" {         GET { findOneSponsor() } }
                }
                "api/{event}/speaker/" { GET { findSpeakersByEvent() } }
            } (request)

    fun findViewById() = HandlerFunction { req ->
        repository.findOne(req.pathVariable("login"))
                .then { u -> ok().render("user", mapOf(Pair("user", u))) }
    }

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOne(req.pathVariable("login"))))
    }

    fun findAllView() = HandlerFunction {
        repository.findAll()
                .collectList()
                .then { u -> ok().render("users",  mapOf(Pair("users", u))) }
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }

    fun findStaff() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findByRole(Role.STAFF)))
    }

    fun findOneStaff() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.STAFF)))
    }

    fun findSpeakers() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findByRole(Role.SPEAKER)))
    }

    fun findSpeakersByEvent() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findByRoleAndEvent(Role.SPEAKER, req.pathVariable("event"))))
    }

    fun findOneSpeaker() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.SPEAKER)))
    }

    fun findSponsors() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findByRole(Role.SPONSOR)))
    }

    fun findOneSponsor() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOneByRole(req.pathVariable("login"), Role.SPONSOR)))
    }

    fun create() = HandlerFunction { req ->
        repository.save(req.bodyToMono(User::class)).single()
                .then { u -> created(URI.create("/api/user/${u.login}"))
                    .contentType(APPLICATION_JSON_UTF8)
                    .body(fromObject(u))
                }
    }
}