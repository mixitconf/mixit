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
import mixit.routes.MustacheTemplate.Search
import mixit.routes.MustacheTemplate.Venue
import mixit.user.model.UserService
import mixit.util.SimpleTemplateLoader
import mixit.util.YearSelector
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok

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

    suspend fun comeToMixitView(req: ServerRequest): ServerResponse =
        SimpleTemplateLoader.openTemplate(
            eventService,
            userService,
            Venue
        )


    suspend fun codeConductView(req: ServerRequest): ServerResponse =
        SimpleTemplateLoader.openTemplate(
            eventService,
            userService,
            CodeOfConduct
        )

    suspend fun accessibilityView(req: ServerRequest): ServerResponse =
        SimpleTemplateLoader.openTemplate(
            eventService,
            userService,
            Accessibility
        )

    suspend fun findFullTextView(req: ServerRequest): ServerResponse =
        SimpleTemplateLoader.openTemplate(
            eventService,
            userService,
            Search
        )
}
