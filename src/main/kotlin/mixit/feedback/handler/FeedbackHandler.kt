package mixit.feedback.handler

import mixit.MixitProperties
import mixit.feedback.model.Feedback
import mixit.feedback.model.FeedbackService
import mixit.security.MixitWebFilter
import mixit.security.MixitWebFilter.Companion.SESSION_EMAIL_KEY
import mixit.talk.model.TalkService
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.mustache.MustacheI18n
import mixit.util.mustache.MustacheI18n.FEEDBACK_COMMENTS
import mixit.util.mustache.MustacheI18n.FEEDBACK_TYPES
import mixit.util.mustache.MustacheI18n.TALK
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheTemplate
import mixit.util.mustache.MustacheTemplate.SpeakerFeedback
import mixit.util.seeOther
import mixit.util.webSession
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class FeedbackHandler(
    private val feedbackService: FeedbackService,
    private val talkService: TalkService,
    private val properties: MixitProperties
) {

    suspend fun findMyFeedbacks(req: ServerRequest): ServerResponse =
        req.webSession().let { session ->
            talkService.findOneOrNull(req.pathVariable("talkId"))!!.let { talk ->
                session.getAttribute<String?>(SESSION_EMAIL_KEY).let { email ->
                    ServerResponse.ok().renderAndAwait(
                        SpeakerFeedback.template,
                        mapOf(
                            TITLE to SpeakerFeedback.title,
                            TALK to talk,
                            FEEDBACK_TYPES to feedbackService.computeTalkFeedback(talk, email),
                            FEEDBACK_COMMENTS to feedbackService.computeTalkFeedbackComment(talk,email)
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
}
