package mixit.controller

import mixit.model.*
import mixit.model.SponsorshipLevel.*
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.RouterFunctionProvider
import mixit.util.language
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.Routes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.time.LocalDate
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

}

fun EventSponsoring.toDto() = SponsorDto(
        this.sponsor.login,
        this.sponsor.company!!,
        this.sponsor.logoUrl!!,
        logoType(this.sponsor.logoUrl),
        logoWebpUrl(this.sponsor.logoUrl)
)

private fun logoWebpUrl(url: String) =
        when {
            url.endsWith("png") -> url.replace("png", "webp")
            url.endsWith("jpg") -> url.replace("jpg", "webp")
            else -> null
        }

private fun logoType(url: String) =
        when {
            url.endsWith("svg") -> "image/svg+xml"
            url.endsWith("png") -> "image/png"
            url.endsWith("jpg") -> "image/jpeg"
            else -> throw IllegalArgumentException("Extension not supported")
        }

class SponsorDto(
        val login: String,
        val company: String,
        val logoUrl: String,
        val logoType: String,
        val logoWebpUrl: String? = null
)

class EventSponsoringDto(
        val level: SponsorshipLevel,
        val sponsor: UserDto,
        val subscriptionDate: LocalDate = LocalDate.now()
)

fun EventSponsoring.toDto(language: Language, markdownConverter: MarkdownConverter) =
        EventSponsoringDto(level, sponsor.toDto(language, markdownConverter), subscriptionDate)
