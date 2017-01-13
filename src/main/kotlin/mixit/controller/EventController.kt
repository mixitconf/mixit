package mixit.controller

import mixit.repository.EventRepository
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok

class EventController(val repository: EventRepository) : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = route(request) {
        accept(APPLICATION_JSON).apply {
            GET("/api/event/") { findAll() }
            GET("/api/event/{login}") { findOne() }
        }
    }

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("login"))))
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }
}
