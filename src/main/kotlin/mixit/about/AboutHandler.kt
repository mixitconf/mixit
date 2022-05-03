package mixit.about

import mixit.event.handler.AdminEventHandler
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel
import mixit.user.handler.toSponsorDto
import mixit.user.model.Role
import mixit.user.model.UserService
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok

@Component
class AboutHandler(val userService: UserService, val eventService: EventService) {

    fun findAboutView(req: ServerRequest) = userService
        .findByRoles(Role.STAFF, Role.STAFF_IN_PAUSE)
        .flatMap { users ->
            eventService
                .findByYear(AdminEventHandler.CURRENT_EVENT.toInt())
                .flatMap { event ->
                    val staff = users.filter { it.role == Role.STAFF }.shuffled()
                    val staffInPause = users.filter { it.role == Role.STAFF_IN_PAUSE }.shuffled()
                    val volunteers = event.volunteers.shuffled()
                    ok().render(
                        "about",
                        mapOf(
                            Pair("volunteers", volunteers.map { it.toDto(req.language()) }),
                            Pair("hasVolunteers", volunteers.isNotEmpty()),
                            Pair("staff", staff.map { it.toDto(req.language()) }),
                            Pair("inactiveStaff", staffInPause.map { it.toDto(req.language()) }),
                            Pair("title", "about.title")
                        )
                    )
                }
        }

    fun faqView(req: ServerRequest) =
        ok().render("faq", mapOf(Pair("title", "faq.title")))

    fun comeToMixitView(req: ServerRequest) =
        eventService
            .findByYear(AdminEventHandler.CURRENT_EVENT.toInt())
            .flatMap { event ->
                event.filterBySponsorLevel(SponsorshipLevel.GOLD).let { sponsors ->
                    ok().render(
                        "come", mapOf(
                            Pair("title", "come.title"),
                            Pair("year", event.year),
                            Pair("sponsors", mapOf(
                                Pair("sponsors-gold", sponsors.map { it.toSponsorDto() }),
                                Pair("sponsors-others", event.sponsors.filterNot { sponsors.contains(it) }.map { it.toSponsorDto() })
                            )),

                        )
                    )
                }
            }

    fun codeConductView(req: ServerRequest) =
        ok().render("code-of-conduct", mapOf(Pair("title", "code-of-conduct.title")))

    fun accessibilityView(req: ServerRequest) =
        ok().render("accessibility", mapOf(Pair("title", "accessibility.title")))

    fun findFullTextView(req: ServerRequest) =
        ok().render("search", mapOf(Pair("title", "search.title")))
}
