package mixit.about

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitApplication
import mixit.event.model.EventService
import mixit.routes.MustacheI18n.SPONSORS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheI18n.YEAR
import mixit.routes.MustacheI18n.YEAR_SELECTOR
import mixit.routes.MustacheTemplate.About
import mixit.routes.MustacheTemplate.Accessibility
import mixit.routes.MustacheTemplate.CodeOfConduct
import mixit.routes.MustacheTemplate.Faq
import mixit.routes.MustacheTemplate.Search
import mixit.routes.MustacheTemplate.Venue
import mixit.user.model.UserService
import mixit.util.YearSelector
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class AboutHandler(
    private val userService: UserService,
    private val eventService: EventService
) {

    suspend fun findAboutView(req: ServerRequest, year: Int): ServerResponse {
        val event = eventService.findByYear(year)

        val staff = event.organizers.shuffled()
        val volunteers = event.volunteers.shuffled()

        val lang = req.language()
        return ok()
            .render(
                About.template,
                mapOf(
                    TITLE to About.title,
                    YEAR to year,
                    YEAR_SELECTOR to YearSelector.create(year, "about", true),
                    SPONSORS to userService.loadSponsors(event),
                    "isCurrent" to (year == MixitApplication.CURRENT_EVENT.toInt()),
                    "volunteers" to volunteers.map { it.toDto(lang) },
                    "hasVolunteers" to volunteers.isNotEmpty(),
                    "staff" to staff.map { it.toDto(lang) }
                )
            )
            .awaitSingle()
    }

    suspend fun faqView(req: ServerRequest): ServerResponse =
        ok()
            .render(Faq.template, mapOf(TITLE to Faq.title))
            .awaitSingle()

    suspend fun comeToMixitView(req: ServerRequest): ServerResponse {
        val event = eventService.findByYear(MixitApplication.CURRENT_EVENT)

        return ok()
            .render(
                Venue.template,
                mapOf(
                    TITLE to Venue.title,
                    YEAR to event.year,
                    SPONSORS to userService.loadSponsors(event)
                )
            )
            .awaitSingle()
    }

    suspend fun codeConductView(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(CodeOfConduct.template, mapOf(TITLE to CodeOfConduct.title))

    suspend fun accessibilityView(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(Accessibility.template, mapOf(TITLE to Accessibility.title))

    suspend fun findFullTextView(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(Search.template, mapOf(TITLE to Search.title))
}
