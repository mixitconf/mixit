package mixit.controller

import mixit.model.Role
import mixit.model.User
import mixit.repository.UserRepository
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.BodyInsertersExtension.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.ServerRequestExtension.bodyToMono
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.net.URI

class UserController(val repository: UserRepository) : RouterFunction<ServerResponse> {

    @Suppress("UNCHECKED_CAST")// TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions.route(
                GET("/user/{login}"), findViewById()).andRoute(
                GET("/user/"), findAllView()).andRoute(
                GET("/api/user/{login}"), findOne()).andRoute(
                GET("/api/user/"), findAll()).andRoute(
                GET("/api/staff/"), findStaff()).andRoute(
                GET("/api/staff/{login}"), findOneStaff()).andRoute(
                GET("/api/speaker/"), findSpeakers()).andRoute(
                GET("/api/{event}/speaker/"), findSpeakersByEvent()).andRoute(
                GET("/api/speaker/{login}"), findOneSpeaker()).andRoute(
                GET("/api/sponsor/"), findSponsors()).andRoute(
                GET("/api/sponsor/{login}"), findOneSponsor()).andRoute(
                POST("/api/user/"), create()
    ).route(request) as Mono<HandlerFunction<ServerResponse>>

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