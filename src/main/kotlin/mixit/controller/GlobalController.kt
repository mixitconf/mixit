package mixit.controller

import mixit.model.SponsorshipLevel.*
import mixit.repository.EventRepository
import mixit.support.LazyRouterFunction
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok

@Controller
class GlobalController(val repository: EventRepository) : LazyRouterFunction() {

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: RouterDsl.() -> Unit = {
        accept(TEXT_HTML).apply {
            GET("/", this@GlobalController::homeView)
        }
        resources("/**", ClassPathResource("static/"))
    }

    fun homeView(req: ServerRequest) = repository.findOne("mixit17")
            .then { events ->
                val sponsors = events.sponsors.groupBy { it.level }
                ok().render("home", mapOf(
                        Pair("sponsors-gold", sponsors[GOLD]),
                        Pair("sponsors-silver", sponsors[SILVER]),
                        Pair("sponsors-hosting", sponsors[HOSTING]),
                        Pair("sponsors-lanyard", sponsors[LANYARD]),
                        Pair("sponsors-party", sponsors[PARTY])
                ))
            }
}
