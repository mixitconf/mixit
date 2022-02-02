package mixit.about

import mixit.blog.handler.toDto
import mixit.blog.repository.PostRepository
import mixit.talk.repository.TalkRepository
import mixit.user.handler.toDto
import mixit.user.model.User
import mixit.user.repository.UserRepository
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok

@Component
class SearchHandler(
    val userRepository: UserRepository,
    val postRepository: PostRepository,
    val talkRepository: TalkRepository
) {

    fun searchFullTextView(req: ServerRequest) =
        req.body(BodyExtractors.toFormData())
            .map { it.toSingleValueMap() }
            .flatMap { formData ->
                if (formData["search"] == null || formData["search"]!!.trim().length < 3) {
                    ok().render(
                        "search",
                        mapOf(
                            Pair("criteria", formData["search"]),
                            Pair("title", "search.title")
                        )
                    )
                } else {
                    val criteria = formData["search"]!!.trim().split(" ")

                    val users = userRepository.findFullText(criteria)
                        .map { user -> user.toDto(req.language(), criteria) }

                    val articles = postRepository.findFullText(criteria).map { article ->
                        val user = User("MiXiT", "MiXiT", "MiXiT", "MiXiT")
                        article.toDto(user, req.language(), criteria)
                    }
//                    val talks = talkRepository.findFullText(criteria)
//                        .map { talk -> talk.toDto(req.language(), emptyList(), false, false, criteria) }

                    ok().render(
                        "search",
                        mapOf(
                            Pair("criteria", formData["search"]),
                            Pair("title", "search.title"),
                            Pair("users", users),
                            Pair("hasUsers", users.hasElements()),
                            Pair("countUsers", users.count()),
//                            Pair("talks", talks),
//                            Pair("hasTalks", talks.hasElements()),
//                            Pair("countTalks", talks.count()),
                            Pair("articles", articles),
                            Pair("hasArticles", articles.hasElements()),
                            Pair("countArticles", articles.count())
                        )
                    )
                }
            }
}
