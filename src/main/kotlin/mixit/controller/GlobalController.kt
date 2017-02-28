package mixit.controller

import mixit.model.EventSponsoring
import mixit.model.SponsorshipLevel.*
import mixit.repository.EventRepository
import mixit.support.RouterFunctionProvider
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.Routes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok

@Controller
class GlobalController(val repository: EventRepository) : RouterFunctionProvider() {

    // TODO Remove this@GlobalController when KT-15667 will be fixed
    override val routes: Routes = {
        accept(TEXT_HTML).route {
            GET("/", this@GlobalController::homeView)
        }
        resources("/**", ClassPathResource("static/"))
    }

    fun homeView(req: ServerRequest) = repository.findOne("mixit17")    // TODO This could benefit from an in-memory cache with like 1H retention (data never change)
            .then { events ->
                val sponsors = events.sponsors.groupBy { it.level }
                ok().render("home", mapOf(
                        Pair("sponsors-gold", sponsors[GOLD]?.map { it.toDto() }),
                        Pair("sponsors-silver", sponsors[SILVER]?.map { it.toDto() }),
                        Pair("sponsors-hosting", sponsors[HOSTING]?.map { it.toDto() }),
                        Pair("sponsors-lanyard", sponsors[LANYARD]?.map { it.toDto() }),
                        Pair("sponsors-party", sponsors[PARTY]?.map { it.toDto() })
                ))
            }

    class SponsorDto(
        val login: String,
        val company: String,
        val logoUrl: String,
        val logoType: String,
        val logoWebpUrl: String? = null
    )

    private fun EventSponsoring.toDto() = SponsorDto(
        this.sponsor.login,
        this.sponsor.company!!,
        this.sponsor.logoUrl!!,
        logoType(this.sponsor.logoUrl),
        logoWebpUrl(this.sponsor.logoUrl)
    )

    private fun logoWebpUrl(url:String): String? {
        if (url.endsWith("png") || url.endsWith("jpg")){
            return url.replace("png", "webp").replace("jpg", "webp")
        }
        return null
    }

    private fun logoType(url:String): String {
        if (url.endsWith("svg")){
            return "image/svg+xml"
        }
        if (url.endsWith("png")){
            return "image/png"
        }
        if (url.endsWith("jpg")){
            return "image/jpeg"
        }
        throw IllegalArgumentException("Extension not supported")
    }

}
