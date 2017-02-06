package mixit.controller

import mixit.model.SponsorshipLevel.*
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller

import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Controller
class SponsorController(val eventRepository: EventRepository, val userRepository: UserRepository) : RouterFunction<ServerResponse> {

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/sponsors/") { findAllView() }
            GET("/sponsors/{id}") { findOneView(req) }
        }
    }

    fun findAllView() = eventRepository.findOne("mixit17").then { events ->
        val sponsors = events.sponsors.groupBy { it.level }

        ok().render("sponsors", mapOf(
            Pair("sponsors-gold", sponsors[GOLD]),
            Pair("sponsors-silver", sponsors[SILVER]),
            Pair("sponsors-hosting", sponsors[HOSTING]),
            Pair("sponsors-lanyard", sponsors[LANYARD]),
            Pair("sponsors-party", sponsors[PARTY])
        ))
    }

    fun findOneView(req: ServerRequest) = userRepository.findOne(req.pathVariable("id")).then { s ->
        ok().render("sponsor", mapOf(Pair("sponsor", s), Pair("description", if (s.longDescription.isEmpty()) s.shortDescription else s.longDescription)))
    }

}
