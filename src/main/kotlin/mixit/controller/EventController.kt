package mixit.controller

import mixit.repository.EventRepository
import mixit.support.RouterFunctionDsl
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok

class EventController(val repository: EventRepository) : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = RouterFunctionDsl {
        GET("/api/event/") { findAll() }
        GET("/api/event/{login}") { findOne() }
    } (request)

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("login"))))
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }
}
