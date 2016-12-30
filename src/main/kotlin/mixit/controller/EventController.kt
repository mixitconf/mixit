package mixit.controller

import mixit.repository.EventRepository
import mixit.repository.SponsorRepository
import mixit.support.fromPublisher
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

class EventController(val repository: EventRepository, val sponsorRepository: SponsorRepository) : RouterFunction<ServerResponse> {

    // TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions
            .route(GET("/api/event/{id}"), findById())
            .andRoute(GET("/api/event/"), findAll())
            .andRoute(GET("/api/sponsor/"), findAllSponsor())
            .route(request) as Mono<HandlerFunction<ServerResponse>>

    fun findById() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findById(req.pathVariable("id").toLong())))
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }

    fun findAllSponsor() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(sponsorRepository.findAll()))
    }
}