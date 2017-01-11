package mixit.controller

import mixit.repository.SessionRepository
import mixit.support.RouterFunctionDsl
import org.springframework.http.MediaType.APPLICATION_JSON

import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok

class SessionController(val repository: SessionRepository) : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = RouterFunctionDsl {
        (GET("/api/session/{login}") and accept(APPLICATION_JSON)) { findOne() }
        (GET("/api/{event}/session/") and accept(APPLICATION_JSON)) { findByEventId() }
    } (request)

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("login"))))
    }

    fun findByEventId() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findByEvent(req.pathVariable("event"))))
    }
}
