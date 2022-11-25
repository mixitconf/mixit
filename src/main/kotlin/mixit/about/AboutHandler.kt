package mixit.about

import kotlinx.coroutines.reactor.awaitSingle
import mixit.event.handler.AdminEventHandler
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel
import mixit.routes.MustacheI18n
import mixit.routes.MustacheTemplate.About
import mixit.routes.MustacheTemplate.Accessibility
import mixit.routes.MustacheTemplate.CodeOfConduct
import mixit.routes.MustacheTemplate.Come
import mixit.routes.MustacheTemplate.Faq
import mixit.routes.MustacheTemplate.Search
import mixit.user.handler.dto.toSponsorDto
import mixit.user.model.Role
import mixit.user.model.UserService
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class AboutHandler(val userService: UserService, val eventService: EventService) {

    suspend fun findAboutView(req: ServerRequest): ServerResponse {
        val users = userService.coFindByRoles(Role.STAFF, Role.STAFF_IN_PAUSE)
        val event = eventService.coFindByYear(AdminEventHandler.CURRENT_EVENT)

        val staff = users.filter { it.role == Role.STAFF }.shuffled()
        val staffInPause = users.filter { it.role == Role.STAFF_IN_PAUSE }.shuffled()
        val volunteers = event.volunteers.shuffled()
        val lang = req.language()
        return ok()
            .render(
                About.template,
                mapOf(
                    MustacheI18n.TITLE to "about.title",
                    "volunteers" to volunteers.map { it.toDto(lang) },
                    "hasVolunteers" to volunteers.isNotEmpty(),
                    "staff" to staff.map { it.toDto(lang) },
                    "inactiveStaff" to staffInPause.map { it.toDto(lang) }
                )
            )
            .awaitSingle()
    }

    suspend fun faqView(req: ServerRequest): ServerResponse =
        ok()
            .render(Faq.template, mapOf(MustacheI18n.TITLE to "faq.title"))
            .awaitSingle()

    suspend fun comeToMixitView(req: ServerRequest): ServerResponse {
        val event = eventService.coFindByYear(AdminEventHandler.CURRENT_EVENT)
        val goldSponsors = event.filterBySponsorLevel(SponsorshipLevel.GOLD)

        return ok()
            .render(
                Come.template,
                mapOf(
                    MustacheI18n.TITLE to "come.title",
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
        ok().renderAndAwait(CodeOfConduct.template, mapOf(MustacheI18n.TITLE to "code-of-conduct.title"))

    suspend fun accessibilityView(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(Accessibility.template, mapOf(MustacheI18n.TITLE to "accessibility.title"))

    suspend fun findFullTextView(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(Search.template, mapOf(MustacheI18n.TITLE to "search.title"))
}
