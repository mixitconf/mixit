package mixit.about

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitApplication
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel
import mixit.routes.MustacheI18n
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheI18n.YEAR
import mixit.routes.MustacheTemplate.About
import mixit.routes.MustacheTemplate.Accessibility
import mixit.routes.MustacheTemplate.CodeOfConduct
import mixit.routes.MustacheTemplate.Come
import mixit.routes.MustacheTemplate.Faq
import mixit.routes.MustacheTemplate.Search
import mixit.user.handler.dto.toSponsorDto
import mixit.user.model.UserService
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class AboutHandler(val userService: UserService, val eventService: EventService) {

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
        val goldSponsors = event.filterBySponsorLevel(SponsorshipLevel.GOLD)

        return ok()
            .render(
                Come.template,
                mapOf(
                    TITLE to Come.title,
                    MustacheI18n.YEAR to event.year,
                    MustacheI18n.SPONSORS to mapOf(
                        "sponsors-gold" to goldSponsors.map { it.toSponsorDto() },
                        "sponsors-others" to event.sponsors.filterNot { goldSponsors.contains(it) }
                            .map { it.toSponsorDto() }
                    )
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
