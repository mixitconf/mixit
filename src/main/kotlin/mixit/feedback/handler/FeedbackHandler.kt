package mixit.feedback.handler

import mixit.MixitProperties
import mixit.feedback.model.Feedback
import mixit.feedback.model.FeedbackService
import mixit.feedback.model.FeedbackService.CommentType
import mixit.security.MixitWebFilter.Companion.SESSION_EMAIL_KEY
import mixit.talk.model.TalkService
import mixit.util.YearSelector
import mixit.util.enumMatcher
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.mustache.MustacheI18n.CRITERIA
import mixit.util.mustache.MustacheI18n.FEEDBACK_COMMENTS
import mixit.util.mustache.MustacheI18n.FEEDBACK_TYPES
import mixit.util.mustache.MustacheI18n.TALK
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheI18n.YEAR
import mixit.util.mustache.MustacheI18n.YEAR_SELECTOR
import mixit.util.mustache.MustacheTemplate.AdminFeedback
import mixit.util.mustache.MustacheTemplate.SpeakerFeedback
import mixit.util.seeOther
import mixit.util.webSession
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import org.springframework.web.reactive.function.server.queryParamOrNull
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class FeedbackHandler(
    private val feedbackService: FeedbackService,
    private val talkService: TalkService,
    private val properties: MixitProperties
) {

    private suspend fun routeToAdminPage(year: String, type: CommentType): ServerResponse =
        ServerResponse.ok().renderAndAwait(
            AdminFeedback.template,
            mapOf(
                TITLE to AdminFeedback.title,
                YEAR to year,
                "type" to type,
                CRITERIA to enumMatcher(type) { type },
                YEAR_SELECTOR to YearSelector.create(year.toInt(), "admin/feedbacks"),
                FEEDBACK_COMMENTS to feedbackService.findComments(type, year)
            )
        )

    suspend fun admin(req: ServerRequest): ServerResponse =
        req.pathVariable("year").let { year ->
            val type = req.queryParamOrNull("type")?.let { CommentType.valueOf(it) } ?: CommentType.Unvalidated
            routeToAdminPage(year, type)
        }


    suspend fun findMyFeedbacks(req: ServerRequest): ServerResponse =
        req.webSession().let { session ->
            talkService.findOneOrNull(req.pathVariable("talkId"))!!.let { talk ->
                session.getAttribute<String?>(SESSION_EMAIL_KEY).let { email ->
                    ServerResponse.ok().renderAndAwait(
                        SpeakerFeedback.template,
                        mapOf(
                            TITLE to SpeakerFeedback.title,
                            TALK to talk,
                            FEEDBACK_TYPES to feedbackService.computeFeedbackForTalk(talk, email),
                            FEEDBACK_COMMENTS to feedbackService.computeCommentsForTalk(talk, email)
                        )
                    )
                }
            }
        }

    suspend fun vote(req: ServerRequest): ServerResponse {
        val email = req.pathVariable("email")
        val talkId = req.pathVariable("talkId")
        val feedback = Feedback.valueOf(req.pathVariable("feedback"))

        return ServerResponse.ok().json().bodyValueAndAwait(
            feedbackService.voteForTalk(talkId, feedback, email)
        )
    }

    suspend fun comment(req: ServerRequest, year: Int): ServerResponse {
        val formData = req.extractFormData()
        val email = formData["email"] ?: throw NotFoundException()
        val talk = talkService.findOneOrNull(formData["talkId"] ?: throw NotFoundException())!!
        val comment = formData["comment"]

        feedbackService.saveCommentForTalk(
            talkId = talk.id,
            comment = comment,
            nonEncryptedUserEmail = email
        )
        return seeOther("${properties.baseUri}/$year/${talk.slug}")
    }

    suspend fun reject(req: ServerRequest): ServerResponse =
        saveFeedbackState(req, accepted = false)

    suspend fun approve(req: ServerRequest): ServerResponse =
        saveFeedbackState(req, accepted = true)

    suspend fun saveFeedbackState(req: ServerRequest, accepted: Boolean): ServerResponse =
        req.webSession().let { session ->
            session.getAttribute<String?>(SESSION_EMAIL_KEY).let { email ->
                val formData = req.extractFormData()
                val feedbackId = req.pathVariable("id")
                val type = formData["type"]?.let { CommentType.valueOf(it) } ?: CommentType.Unvalidated
                val year = formData["year"] ?: throw NotFoundException()

                feedbackService.saveFeedbackState(email!!, feedbackId, type, accepted)
                routeToAdminPage(year, type)
            }
        }
}
