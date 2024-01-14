package mixit.feedback.model

import mixit.security.model.Cryptographer
import mixit.talk.model.CachedTalk
import mixit.talk.model.Talk
import org.springframework.stereotype.Service

data class FeedbackCount(val count: Int, val selectedByCurrentUser: Boolean)

@Service
class FeedbackService(
    private val userFeedbackService: UserFeedbackService,
    private val cryptographer: Cryptographer
) {

    suspend fun computeTalkFeedback(talk: CachedTalk, nonEncryptedUserEmail: String?): List<Pair<Feedback, FeedbackCount>> {
        val feedbacks = userFeedbackService.findByTalk(talk.id)
        // We need to compute the different scores
        return Feedback.entries
            .filter { it.formats.contains(talk.format) }
            .map { feedback ->
                val currentUserFeedback = nonEncryptedUserEmail?.let { email ->
                    feedbacks.firstOrNull { it.user.email == cryptographer.encrypt(email) }?.notes?.contains(feedback)
                } ?: false

                val feedbackNotes = feedbacks.mapNotNull { userFeedback ->
                    userFeedback.notes.firstOrNull { it == feedback }
                }
                feedback to FeedbackCount(
                    count = feedbackNotes.count(),
                    selectedByCurrentUser = currentUserFeedback
                )
            }
            .sortedBy { it.first.sort }
    }

}
