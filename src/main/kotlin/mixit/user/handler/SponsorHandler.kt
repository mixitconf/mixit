package mixit.user.handler

import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel.Companion.sponsorshipLevels
import mixit.talk.model.TalkService
import mixit.talk.model.Topic
import mixit.user.handler.dto.toSpeakerStarDto
import mixit.user.model.UserService
import mixit.util.YearSelector
import mixit.util.language
import mixit.util.mustache.MustacheI18n
import mixit.util.mustache.MustacheTemplate.Home
import mixit.util.mustache.MustacheTemplate.Sponsors
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.renderAndAwait

@Suppress("CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION")
@Component
class SponsorHandler(
    private val eventService: EventService,
    private val userService: UserService,
    private val talkService: TalkService
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
            MustacheI18n.YEAR_SELECTOR to YearSelector.create(year, "sponsors", true),
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
            MustacheI18n.YEAR to year,
            MustacheI18n.YEAR_SELECTOR to YearSelector.create(year, template, true),
            MustacheI18n.EVENT to event,
            "title" to if (template != "sponsors") title else "$title|$year"
        )
        if (template == Home.template) {
            context["aliens"] = talkService
                .findNKeynoteByTopic(3, Topic.ALIENS.value)
                .map { it.toDto(req.language()) }
        }

        return ServerResponse.ok().renderAndAwait(template, context)
    }
}
