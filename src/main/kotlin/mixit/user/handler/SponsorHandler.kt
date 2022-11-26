package mixit.user.handler

import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel
import mixit.event.model.SponsorshipLevel.Companion.sponsorshipLevels
import mixit.event.model.SponsorshipLevel.GOLD
import mixit.event.model.SponsorshipLevel.LANYARD
import mixit.event.model.SponsorshipLevel.PARTY
import mixit.routes.MustacheI18n
import mixit.routes.MustacheTemplate.Sponsors
import mixit.user.handler.dto.toSpeakerStarDto
import mixit.user.handler.dto.toSponsorDto
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.renderAndAwait

@Suppress("CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION")
@Component
class SponsorHandler(private val eventService: EventService) {

    suspend fun viewSponsors(req: ServerRequest, year: Int = CURRENT_EVENT.toInt()): ServerResponse {
        val event = eventService.coFindByYear(year)
        val lg = req.language()

        val levels: List<Pair<String, *>> = sponsorshipLevels()
            .flatMap { level ->
                val sponsors = event.filterBySponsorLevel(GOLD).map { it.toEventSponsoringDto(lg) }
                listOf(
                    "sponsors-${level.name.lowercase()}" to sponsors,
                    "has-sponsors-${level.name.lowercase()}" to sponsors.isNotEmpty()
                )
            }

        val context = levels.toMap() + mapOf(
            MustacheI18n.YEAR to year,
            MustacheI18n.TITLE to "sponsors.title|$year"
        )
        return ServerResponse.ok().renderAndAwait(Sponsors.template, context)
    }

    suspend fun viewWithSponsors(
        req: ServerRequest,
        template: String,
        title: String? = null,
        spotLights: Array<SponsorshipLevel> = arrayOf(LANYARD, GOLD, PARTY),
        year: Int = CURRENT_EVENT.toInt(),
    ): ServerResponse {
        val event = eventService.coFindByYear(year)
        val mainSponsors = event.filterBySponsorLevel(*spotLights)
        val otherSponsors = event.sponsors.filterNot { mainSponsors.contains(it) }

        val context = mutableMapOf(
            "year" to year,
            "title" to if (template != "sponsors") title else "$title|$year",
            "sponsors-main" to mainSponsors.map { it.toSponsorDto() },
            "sponsors-others" to otherSponsors.map { it.toSponsorDto() }
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
