package mixit.feedback.model

import mixit.MixitApplication
import mixit.security.model.Cryptographer
import mixit.talk.model.CachedTalk
import mixit.talk.model.TalkService
import mixit.user.model.UserService
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

data class FeedbackCount(val count: Int, val selectedByCurrentUser: Boolean)

data class UserFeedbackComment(
    val login: String?,
    val userComment: String?,
    val otherComments: List<FeedbackCommentDto>
)

data class FeedbackCommentDto(
    val comment: String,
    val plusCount: Int,
    val plusSelectedByCurrentUser: Boolean,
    val minusCount: Int,
    val minusSelectedByCurrentUser: Boolean
)

@Service
class FeedbackService(
    private val userFeedbackService: UserFeedbackService,
    private val userService: UserService,
    private val talkService: TalkService,
    private val cryptographer: Cryptographer
) {

    suspend fun computeTalkFeedback(
        talk: CachedTalk,
        nonEncryptedUserEmail: String?
    ): List<Pair<Feedback, FeedbackCount>> {
        val feedbacks = userFeedbackService.findByTalk(talk.id)
        // We need to compute the different scores
        return Feedback.entries
            .filter { it.formats.contains(talk.format) }
            .map { feedback ->
                val currentUserFeedback = nonEncryptedUserEmail?.let { email ->
                    feedbacks.any { it.user.email == cryptographer.encrypt(email) && it.notes.contains(feedback) }
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

    suspend fun computeTalkFeedbackComment(
        talk: CachedTalk,
        nonEncryptedUserEmail: String?
    ): UserFeedbackComment {
        val user = nonEncryptedUserEmail?.let {
            userService.findOneByNonEncryptedEmailOrNull(it)
        }

        try {
            val feedbacks = userFeedbackService.findByTalk(talk.id)

            val otherComments = feedbacks
                .mapNotNull { it.comment }
                .map { comment ->
                    FeedbackCommentDto(
                        comment = comment.comment,
                        plusCount = comment.feedbackPlus.count(),
                        plusSelectedByCurrentUser = user?.let { comment.feedbackPlus.contains(it.login) } ?: false,
                        minusCount = comment.feedbackMinus.count(),
                        minusSelectedByCurrentUser = user?.let { comment.feedbackMinus.contains(it.login) } ?: false,
                    )
                }

            return UserFeedbackComment(
                login = user?.login,
                userComment = user?.let { u -> feedbacks.first { it.user.login == u.login }.comment?.comment },
                otherComments = otherComments
            )
        } catch (e: Exception) {
            return UserFeedbackComment(
                login = user?.login,
                userComment = null,
                otherComments = emptyList()
            )
        }

    }

    suspend fun voteForTalk(
        talkId: String,
        feedback: Feedback,
        nonEncryptedUserEmail: String
    ): FeedbackCount {
        val encryptedEmail = cryptographer.encrypt(nonEncryptedUserEmail)!!
        val userFeedbackOnTalk = userFeedbackService
            .findByTalk(talkId)
            .firstOrNull {
                it.user.email == cryptographer.encrypt(nonEncryptedUserEmail)
            }
        val userHasAlreadyFeed = userFeedbackOnTalk?.notes?.contains(feedback) ?: false

        val userFeedbackOnTalkToPersist = userFeedbackOnTalk
            ?.copy(
                notes = if (userHasAlreadyFeed) userFeedbackOnTalk.notes.filter { it != feedback } else
                    userFeedbackOnTalk.notes + feedback
            )
            ?.toUserFeedback()
            ?: UserFeedback(
                encryptedEmail = encryptedEmail,
                talkId = talkId,
                event = MixitApplication.CURRENT_EVENT,
                creationInstant = Instant.now(),
                notes = listOf(feedback),
                comment = null,
                id = UUID.randomUUID().toString()
            )

        userFeedbackService.save(userFeedbackOnTalkToPersist)

        val feedbackNotes = userFeedbackService.findByTalk(talkId).mapNotNull { userFeedback ->
            userFeedback.notes.firstOrNull { it == feedback }
        }

        return FeedbackCount(
            count = feedbackNotes.size,
            selectedByCurrentUser = (userFeedbackOnTalk == null)
        )
    }

    suspend fun saveCommentForTalk(
        talkId: String,
        comment: String?,
        nonEncryptedUserEmail: String
    ): UserFeedbackComment {
        val encryptedEmail = cryptographer.encrypt(nonEncryptedUserEmail)!!
        val userFeedbackOnTalk = userFeedbackService
            .findByTalk(talkId)
            .firstOrNull {
                it.user.email == cryptographer.encrypt(nonEncryptedUserEmail)
            }

        val userFeedbackOnTalkToPersist: UserFeedback? =
            if (comment == null && userFeedbackOnTalk == null) {
                null
            } else if (userFeedbackOnTalk != null && comment == null) {
                userFeedbackOnTalk.copy(comment = null).toUserFeedback()
            } else if (userFeedbackOnTalk == null && comment != null) {
                UserFeedback(
                    encryptedEmail = encryptedEmail,
                    talkId = talkId,
                    event = MixitApplication.CURRENT_EVENT,
                    creationInstant = Instant.now(),
                    notes = emptyList(),
                    comment = FeedbackComment.create(comment),
                    id = UUID.randomUUID().toString()
                )
            } else {
                userFeedbackOnTalk!!.copy(comment = FeedbackComment.create(comment!!)).toUserFeedback()
            }

        if (userFeedbackOnTalkToPersist != null) {
            userFeedbackService.save(userFeedbackOnTalkToPersist)
        }

        return computeTalkFeedbackComment(talkService.findOneOrNull(talkId)!!, nonEncryptedUserEmail)
    }
}
