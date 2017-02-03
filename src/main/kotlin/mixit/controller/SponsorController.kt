package mixit.controller

import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.SessionRepository
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller

import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Controller
class SponsorController(val repository: EventRepository) : RouterFunction<ServerResponse> {

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/sponsors/") { findAllView() }
        }
    }

    fun findAllView() = repository.findOne("mixit17")
            .then { events ->
                val sponsors = events.sponsors.groupBy { it.level }

                ok().render("sponsors", mapOf(
                        Pair("sponsorsGold", sponsors.get(SponsorshipLevel.GOLD)),
                        Pair("sponsorsSilver", sponsors.get(SponsorshipLevel.SILVER))
                ))
            }


}
