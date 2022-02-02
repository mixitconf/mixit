package mixit.about

import mixit.user.handler.toDto
import mixit.user.model.Role
import mixit.user.repository.UserRepository
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok

@Component
class AboutHandler(val userRepository: UserRepository) {

    fun findAboutView(req: ServerRequest) = userRepository
        .findByRoles(listOf(Role.STAFF, Role.STAFF_IN_PAUSE))
        .collectList()
        .flatMap { users ->
            val staff = users.filter { it.role == Role.STAFF }.shuffled()
            val staffInPause = users.filter { it.role == Role.STAFF_IN_PAUSE }.shuffled()
            ok().render(
                "about",
                mapOf(
                    Pair("staff", staff.map { it.toDto(req.language()) }),
                    Pair("inactiveStaff", staffInPause.map { it.toDto(req.language()) }),
                    Pair("title", "about.title")
                )
            )
        }

    fun faqView(req: ServerRequest) =
        ok().render("faq", mapOf(Pair("title", "faq.title")))

    fun comeToMixitView(req: ServerRequest) =
        ok().render("come", mapOf(Pair("title", "come.title")))

    fun codeConductView(req: ServerRequest) =
        ok().render("code-of-conduct", mapOf(Pair("title", "code-of-conduct.title")))

    fun accessibilityView(req: ServerRequest) =
        ok().render("accessibility", mapOf(Pair("title", "accessibility.title")))

    fun findFullTextView(req: ServerRequest) =
        ok().render("search", mapOf(Pair("title", "search.title")))
}
