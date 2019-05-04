package mixit.web.handler

import mixit.model.Role
import mixit.model.User
import mixit.repository.PostRepository
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.util.*


@Component
class GlobalHandler(val userRepository: UserRepository,
                    val postRepository: PostRepository,
                    val talkRepository: TalkRepository,
                    val markdownConverter: MarkdownConverter) {

    fun findAboutView(req: ServerRequest) = userRepository
            .findByRoles(listOf(Role.STAFF, Role.STAFF_IN_PAUSE))
            .collectList()
            .flatMap {
                val usersByRole = it.groupBy { it.role }

                Collections.shuffle(usersByRole[Role.STAFF])
                if(usersByRole[Role.STAFF_IN_PAUSE] != null){
                    Collections.shuffle(usersByRole[Role.STAFF_IN_PAUSE])
                }

                ok().render("about", mapOf(
                        Pair("staff", usersByRole[Role.STAFF]?.map { it.toDto(req.language(), markdownConverter) }),
                        Pair("inactiveStaff", usersByRole[Role.STAFF_IN_PAUSE]?.map { it.toDto(req.language(), markdownConverter) }),
                        Pair("title", "about.title")))
            }

    fun faqView(req: ServerRequest) = ok().render("faq", mapOf(Pair("title", "faq.title")))

    fun comeToMixitView(req: ServerRequest) = ok().render("come", mapOf(Pair("title", "come.title")))

    fun scheduleView(req: ServerRequest) = ok().render("schedule", mapOf(Pair("title", "schedule.title")))

    fun speakerView(req: ServerRequest) = ok().render("speaker", mapOf(Pair("title", "speaker.title")))

    fun accessibilityView(req: ServerRequest) = ok().render("accessibility", mapOf(Pair("title", "accessibility.title")))

    fun findFullTextView(req: ServerRequest) = ok().render("search", mapOf(Pair("title", "search.title")))

    fun searchFullTextView(req: ServerRequest) =
            req.body(BodyExtractors.toFormData()).flatMap {
                val formData = it.toSingleValueMap()

                if(formData["search"] == null || formData["search"]!!.trim().length < 3){
                    ok().render("search", mapOf(
                            Pair("criteria", formData["search"]),
                            Pair("title", "search.title")
                    ))
                }
                else {
                    val criteria = formData["search"]!!.trim().split(" ")

                    val users = userRepository.findFullText(criteria).map { it.toDto(req.language(), markdownConverter, criteria) }
                    val articles = postRepository.findFullText(criteria).map { it.toDto(User("MiXiT", "MiXiT", "MiXiT", "MiXiT"), req.language(), criteria) }
                    val talks = talkRepository.findFullText(criteria).map { it.toDto(req.language(), emptyList(), false, false, criteria) }

                    ok().render("search", mapOf(
                            Pair("criteria", formData["search"]),
                            Pair("title", "search.title"),
                            Pair("users",  users),
                            Pair("hasUsers",  users.hasElements()),
                            Pair("countUsers",  users.count()),
                            Pair("talks", talks),
                            Pair("hasTalks",  talks.hasElements()),
                            Pair("countTalks",  talks.count()),
                            Pair("articles", articles),
                            Pair("hasArticles",  articles.hasElements()),
                            Pair("countArticles",  articles.count())
                    ))
                }
            }
}

