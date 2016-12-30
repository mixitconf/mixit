package mixit.controller

import mixit.repository.SponsorRepository
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import org.springframework.web.reactive.function.BodyInsertersExtension.fromPublisher

class SponsorController(val repository: SponsorRepository) : RouterFunction<ServerResponse> {

    // TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions
            .route(GET("/api/sponsor/{id}"), findOne())
            .andRoute(GET("/api/sponsor/"), findAll())
            .route(request) as Mono<HandlerFunction<ServerResponse>>

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("id"))))
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }
}