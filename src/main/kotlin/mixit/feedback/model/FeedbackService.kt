package mixit.feedback.model

import mixit.MixitApplication
import mixit.security.model.Cryptographer
import mixit.talk.model.CachedTalk
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
    private val cryptographer: Cryptographer
) {

    suspend fun computeUserTalkFeedback(
        talk: CachedTalk,
        nonEncryptedUserEmail: String?
    ): List<Pair<Feedback, FeedbackCount>> {
        if (nonEncryptedUserEmail == null) {
            return emptyList()
        }

        val currentUserFeedback = userFeedbackService
            .findByTalk(talk.id)
            .firstOrNull { it.user.email == cryptographer.encrypt(nonEncryptedUserEmail) }

        // We need to compute the different scores
        return Feedback.entries
            .filter { it.formats.contains(talk.format) }
            .map { feedback ->
                val hasCurrentUserFeedback = currentUserFeedback?.notes?.contains(feedback) ?: false
                feedback to FeedbackCount(
                    count = if (hasCurrentUserFeedback) 1 else 0,
                    selectedByCurrentUser = hasCurrentUserFeedback
                )
            }
            .sortedBy { it.first.sort }
    }

    suspend fun computeTalkFeedback(
        talk: CachedTalk,
        nonEncryptedUserEmail: String?
    ): List<Pair<Feedback, FeedbackCount>> {
        val user = nonEncryptedUserEmail?.let { userService.findOneByNonEncryptedEmailOrNull(it) }
        if (user == null || talk.speakers.none { it.login == user.login }) {
            return emptyList()
        }
        val feedbacks = userFeedbackService.findByTalk(talk.id)

        // We need to compute the different scores
        return Feedback.entries
            .filter { it.formats.contains(talk.format) }
            .map { feedback ->
                val feedbackNotes = feedbacks.mapNotNull { userFeedback ->
                    userFeedback.notes.firstOrNull { it == feedback }
                }
                feedback to FeedbackCount(
                    count = feedbackNotes.count(),
                    selectedByCurrentUser = false
                )
            }
            .sortedBy { it.first.sort }
    }

    suspend fun computeUserTalkFeedbackComment(
        talk: CachedTalk,
        nonEncryptedUserEmail: String?
    ): UserFeedbackComment {
        val user = nonEncryptedUserEmail?.let { userService.findOneByNonEncryptedEmailOrNull(it) }

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

    suspend fun computeTalkFeedbackComment(
        talk: CachedTalk,
        nonEncryptedUserEmail: String?
    ): UserFeedbackComment? {
        val user = nonEncryptedUserEmail?.let { userService.findOneByNonEncryptedEmailOrNull(it) }
        if (user == null || talk.speakers.none { it.login == user.login }) {
            return null
        }
        val feedbacks = userFeedbackService
            .findByTalk(talk.id)
            .mapNotNull { it.comment }
            .filter { it.approvedInstant != null && it.approvedByLogin != null }
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
            login = null,
            userComment = null,
            otherComments = feedbacks
        )
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
                it.user.email == encryptedEmail
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
            selectedByCurrentUser = userFeedbackOnTalkToPersist.notes.contains(feedback)
        )
    }

    suspend fun saveCommentForTalk(
        talkId: String,
        comment: String?,
        nonEncryptedUserEmail: String
    ) {
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
    }
}
