package mixit.controller

import mixit.repository.SessionRepository
import mixit.support.invoke

import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.BodyInsertersExtension.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok

class SessionController(val repository: SessionRepository) : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) =
            "/api/" {
                "session/{login}" {  GET { findOne() } }
                "{event}/session/" { GET { findByEventId() } }
            } (request)

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("login"))))
    }

    fun findByEventId() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findByEvent(req.pathVariable("event"))))
    }
}
