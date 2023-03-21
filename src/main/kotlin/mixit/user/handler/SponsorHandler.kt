package mixit.user.handler

import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel.Companion.sponsorshipLevels
import mixit.routes.MustacheI18n
import mixit.routes.MustacheTemplate.Sponsors
import mixit.user.handler.dto.toSpeakerStarDto
import mixit.user.model.UserService
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.renderAndAwait

@Suppress("CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION")
@Component
class SponsorHandler(
    private val eventService: EventService,
    private val userService: UserService
) {

    suspend fun viewSponsors(req: ServerRequest, year: Int = CURRENT_EVENT.toInt()): ServerResponse {
        val event = eventService.findByYear(year)
        val lg = req.language()

        val levels: List<Pair<String, *>> = sponsorshipLevels()
            .flatMap { level ->
                val sponsors = event.filterBySponsorLevel(level).map { it.toEventSponsoringDto(lg) }
                listOf(
                    "sponsors-${level.name.lowercase()}" to sponsors,
                    "has-sponsors-${level.name.lowercase()}" to sponsors.isNotEmpty()
                )
            }

        val context = levels.toMap() + mapOf(
            MustacheI18n.YEAR to year,
            MustacheI18n.TITLE to "sponsors.title|$year",
            "isCurrent" to (year == CURRENT_EVENT.toInt())
        )
        return ServerResponse.ok().renderAndAwait(Sponsors.template, context)
    }

    suspend fun viewWithSponsors(
        req: ServerRequest,
        template: String,
        title: String? = null,
        year: Int = CURRENT_EVENT.toInt(),
    ): ServerResponse {
        val event = eventService.findByYear(year)

        val context = mutableMapOf(
            MustacheI18n.SPONSORS to userService.loadSponsors(event),
            "year" to year,
            "title" to if (template != "sponsors") title else "$title|$year"
        )
        if (template == "home") {
            context["stars-old"] = event.speakerStarInHistory
                .shuffled()
                .map { it.toSpeakerStarDto() }
                .subList(0, 6)
            context["stars-current"] = event.speakerStarInCurrent
                .shuffled()
                .map { it.toSpeakerStarDto() }
                .subList(0, 6)
        }

        return ServerResponse.ok().renderAndAwait(template, context)
    }
}
