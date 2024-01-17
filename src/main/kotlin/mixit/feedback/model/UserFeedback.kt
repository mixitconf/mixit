package mixit.feedback.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class UserFeedback(
    val encryptedEmail: String,
    val talkId: String,
    val event: String,
    val creationInstant: Instant,
    val notes: List<Feedback> = emptyList(),
    val comment: FeedbackComment? = null,
    @Id val id: String? = null
)

data class FeedbackComment(
    val comment: String,
    val disapprovedInstant: Instant? = null,
    val disapprovedByLogin: String? = null,
    val approvedInstant: Instant? = null,
    val approvedByLogin: String? = null,
    val feedbackPlus: List<String> = emptyList(),
    val feedbackMinus: List<String> = emptyList()
) {
    companion object {
        fun create(comment: String) = FeedbackComment(comment)
    }
}
