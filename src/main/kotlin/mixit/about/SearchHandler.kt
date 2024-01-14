package mixit.about

import mixit.blog.handler.toDto
import mixit.blog.repository.PostRepository
import mixit.faq.repository.QuestionSectionRepository
import mixit.util.mustache.MustacheI18n.ARTICLES
import mixit.util.mustache.MustacheI18n.COUNT_ARTICLES
import mixit.util.mustache.MustacheI18n.COUNT_QUESTIONS
import mixit.util.mustache.MustacheI18n.COUNT_TALKS
import mixit.util.mustache.MustacheI18n.COUNT_USERS
import mixit.util.mustache.MustacheI18n.CRITERIA
import mixit.util.mustache.MustacheI18n.FAQ
import mixit.util.mustache.MustacheI18n.HAS_ARTICLES
import mixit.util.mustache.MustacheI18n.HAS_QUESTION
import mixit.util.mustache.MustacheI18n.HAS_TALKS
import mixit.util.mustache.MustacheI18n.HAS_USERS
import mixit.util.mustache.MustacheI18n.TALKS
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheI18n.USERS
import mixit.util.mustache.MustacheTemplate.Search
import mixit.talk.model.CachedTalk
import mixit.talk.model.Talk
import mixit.talk.repository.TalkRepository
import mixit.user.handler.dto.toDto
import mixit.user.model.User
import mixit.user.repository.UserRepository
import mixit.util.extractFormData
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class SearchHandler(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val talkRepository: TalkRepository,
    private val questionSectionRepository: QuestionSectionRepository
) {

    suspend fun searchFullTextView(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()

        if (formData["search"] == null || formData["search"]!!.trim().length < 3) {
            val params = mapOf(
                TITLE to "search.title",
                CRITERIA to formData["search"]
            )
            return ok().renderAndAwait(Search.template, params)
        } else {
            val criteria = formData["search"]!!.trim().split(" ").filter { it.length > 2 }

            val users = userRepository.findFullText(criteria).map { user ->
                user.toDto(req.language(), criteria)
            }
            val articles = postRepository.findFullText(criteria).map { article ->
                val user = User.mixit()
                article.toDto(user, req.language(), criteria)
            }
            val talks = talkRepository.findFullText(criteria).map { talk: Talk ->
                CachedTalk(talk, emptyList()).toDto(req.language(), false)
            }
            val questions = questionSectionRepository.findFullText(criteria).mapNotNull { question ->
                question.markFoundOccurrences(req.language(), criteria)
            }

            val params = mapOf(
                TITLE to "search.title",
                CRITERIA to formData["search"],
                USERS to users,
                COUNT_USERS to users.count(),
                HAS_USERS to users.isNotEmpty(),
                TALKS to talks,
                HAS_TALKS to talks.isNotEmpty(),
                COUNT_TALKS to talks.count(),
                ARTICLES to articles,
                HAS_ARTICLES to articles.isNotEmpty(),
                COUNT_ARTICLES to articles.count(),
                COUNT_QUESTIONS to questions.count(),
                HAS_QUESTION to questions.isNotEmpty(),
                FAQ to questions

            )
            return ok().renderAndAwait(Search.template, params)
        }
    }
}
