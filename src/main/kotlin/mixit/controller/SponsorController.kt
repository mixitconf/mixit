package mixit.controller

import mixit.model.SponsorshipLevel.*
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import mixit.support.LazyRouterFunction
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller

import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Controller
class SponsorController(val eventRepository: EventRepository, val userRepository: UserRepository) : LazyRouterFunction() {

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: Routes.() -> Unit = {
        accept(TEXT_HTML).route {
            GET("/sponsors/", this@SponsorController::findAllView)
        }
    }

    fun findAllView(req: ServerRequest) = eventRepository.findOne("mixit17").then { events ->
        val sponsors = events.sponsors.groupBy { it.level }

        ok().render("sponsors", mapOf(
            Pair("sponsors-gold", sponsors[GOLD]),
            Pair("sponsors-silver", sponsors[SILVER]),
            Pair("sponsors-hosting", sponsors[HOSTING]),
            Pair("sponsors-lanyard", sponsors[LANYARD]),
            Pair("sponsors-party", sponsors[PARTY])
        ))
    }

}
