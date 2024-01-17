package mixit.feedback.handler

import mixit.feedback.model.Feedback
import mixit.feedback.model.FeedbackService
import mixit.talk.handler.TalkHandler
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json

@Component
class FeedbackHandler(
    private val feedbackService: FeedbackService,
    private val talkHandler: TalkHandler
) {
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
        val talkId = formData["talkId"] ?: throw NotFoundException()
        val comment = formData["comment"]

        feedbackService.saveCommentForTalk(
            talkId = talkId,
            comment = comment,
            nonEncryptedUserEmail = email
        )
        return talkHandler.findOneView(req, year)
    }
}
