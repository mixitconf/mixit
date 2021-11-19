package mixit.web.handler

import mixit.model.Role
import mixit.model.User
import mixit.repository.PostRepository
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.language
import mixit.util.shuffle
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Component
class GlobalHandler(
    val userRepository: UserRepository,
    val postRepository: PostRepository,
    val talkRepository: TalkRepository,
    val markdownConverter: MarkdownConverter
) {

    fun findAboutView(req: ServerRequest) = userRepository
        .findByRoles(listOf(Role.STAFF, Role.STAFF_IN_PAUSE))
        .collectList()
        .flatMap { users ->
            val staff = users.filter { it.role == Role.STAFF }.shuffled()
            val staffInPause = users.filter { it.role == Role.STAFF_IN_PAUSE }.shuffled()
            ok().render(
                "about", mapOf(
                    Pair("staff", staff.map { it.toDto(req.language(), markdownConverter) }),
                    Pair("inactiveStaff", staffInPause.map { it.toDto(req.language(), markdownConverter) }),
                    Pair("title", "about.title")
                )
            )
        }

    fun faqView(req: ServerRequest) = ok().render("faq", mapOf(Pair("title", "faq.title")))

    fun comeToMixitView(req: ServerRequest) = ok().render("come", mapOf(Pair("title", "come.title")))

    fun scheduleView(req: ServerRequest) = ok().render("schedule", mapOf(Pair("title", "schedule.title")))

    fun codeConductView(req: ServerRequest) =
        ok().render("code-of-conduct", mapOf(Pair("title", "code-of-conduct.title")))

    fun speakerView(req: ServerRequest) = ok().render("speaker", mapOf(Pair("title", "speaker.title")))

    fun accessibilityView(req: ServerRequest) =
        ok().render("accessibility", mapOf(Pair("title", "accessibility.title")))

    fun findFullTextView(req: ServerRequest) = ok().render("search", mapOf(Pair("title", "search.title")))

    fun searchFullTextView(req: ServerRequest) =
        req.body(BodyExtractors.toFormData()).flatMap {
            val formData = it.toSingleValueMap()

            if (formData["search"] == null || formData["search"]!!.trim().length < 3) {
                ok().render(
                    "search", mapOf(
                        Pair("criteria", formData["search"]),
                        Pair("title", "search.title")
                    )
                )
            } else {
                val criteria = formData["search"]!!.trim().split(" ")

                val users = userRepository.findFullText(criteria)
                    .map { user -> user.toDto(req.language(), markdownConverter, criteria) }

                val articles = postRepository.findFullText(criteria).map { article ->
                    val user = User("MiXiT", "MiXiT", "MiXiT", "MiXiT")
                    article.toDto(user, req.language(), criteria)
                }
                val talks = talkRepository.findFullText(criteria)
                    .map { talk -> talk.toDto(req.language(), emptyList(), false, false, criteria) }

                ok().render(
                    "search", mapOf(
                        Pair("criteria", formData["search"]),
                        Pair("title", "search.title"),
                        Pair("users", users),
                        Pair("hasUsers", users.hasElements()),
                        Pair("countUsers", users.count()),
                        Pair("talks", talks),
                        Pair("hasTalks", talks.hasElements()),
                        Pair("countTalks", talks.count()),
                        Pair("articles", articles),
                        Pair("hasArticles", articles.hasElements()),
                        Pair("countArticles", articles.count())
                    )
                )
            }
        }
}

