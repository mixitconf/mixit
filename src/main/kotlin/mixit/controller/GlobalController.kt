package mixit.controller

import mixit.model.EventSponsoring
import mixit.model.Role
import mixit.model.SponsorshipLevel.*
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import mixit.support.MarkdownConverter
import mixit.support.RouterFunctionProvider
import mixit.support.language
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.Routes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.util.*

@Controller
class GlobalController(val userRepository: UserRepository,
                       val eventRepository: EventRepository,
                       val markdownConverter: MarkdownConverter) : RouterFunctionProvider() {

    override val routes: Routes = {
        accept(TEXT_HTML).route {
            GET("/", this@GlobalController::homeView)
            GET("/about", this@GlobalController::findAboutView)
        }
        resources("/**", ClassPathResource("static/"))
    }

    fun homeView(req: ServerRequest) = eventRepository.findOne("mixit17").then { events -> // TODO This could benefit from an in-memory cache with like 1H retention (data never change)
        val sponsors = events.sponsors.groupBy { it.level }
        ok().render("home", mapOf(
                Pair("sponsors-gold", sponsors[GOLD]?.map { it.toDto() }),
                Pair("sponsors-silver", sponsors[SILVER]?.map { it.toDto() }),
                Pair("sponsors-hosting", sponsors[HOSTING]?.map { it.toDto() }),
                Pair("sponsors-lanyard", sponsors[LANYARD]?.map { it.toDto() }),
                Pair("sponsors-party", sponsors[PARTY]?.map { it.toDto() })
        ))
    }

    fun findAboutView(req: ServerRequest) = userRepository.findByRole(Role.STAFF).collectList().then { u ->
        val users = u.map { it.toDto(req.language(), markdownConverter) }
        Collections.shuffle(users)
        ok().render("about",  mapOf(Pair("staff", users)))
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
