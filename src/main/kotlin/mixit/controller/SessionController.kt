package mixit.controller

import mixit.repository.SessionRepository

import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.BodyInsertersExtension.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

class SessionController(val repository: SessionRepository) : RouterFunction<ServerResponse> {

    @Suppress("UNCHECKED_CAST") // TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions.route(
            GET("/session/"), findAllView()).andRoute(
            GET("/session/{id}"), findOneView()).andRoute(
            GET("/api/session/{id}"), findOne()).andRoute(
            GET("/api/{event}/session/"), findByEventId()
    ).route(request) as Mono<HandlerFunction<ServerResponse>>

    fun findAllView() = HandlerFunction {
        repository.findAll()
                .collectList()
                .then { session -> ok().render("sessions",  mapOf(Pair("sessions", session))) }
    }

    fun findOneView() = HandlerFunction { req ->
        repository.findOne(req.pathVariable("id"))
                .then { session -> ok().render("session", mapOf(Pair("session", session))) }
    }

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("id"))))
    }

    fun findByEventId() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findByEvent(req.pathVariable("event"))))
    }
}
