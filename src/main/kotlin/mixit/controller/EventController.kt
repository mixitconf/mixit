package mixit.controller

import mixit.repository.EventRepository
import mixit.support.invoke
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.BodyInsertersExtension.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok

class EventController(val repository: EventRepository) : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) =
            "/api/event" {
                "/" {       GET { findAll() } }
                "/{login}" { GET { findOne() } }
            } (request)

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("login"))))
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }
}
