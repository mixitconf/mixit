package mixit.feedback.model

import java.time.Instant
import java.util.UUID
import mixit.MixitApplication
import mixit.feedback.model.FeedbackService.CommentType.All
import mixit.feedback.model.FeedbackService.CommentType.Rejected
import mixit.feedback.model.FeedbackService.CommentType.Unvalidated
import mixit.feedback.model.FeedbackService.CommentType.Validated
import mixit.security.model.Cryptographer
import mixit.talk.handler.TalkDto
import mixit.talk.model.CachedTalk
import mixit.talk.model.Language
import mixit.user.model.UserService
import mixit.user.model.desanonymize
import mixit.util.extractFormData
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Service

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

    suspend fun computeUserFeedbackForTalk(
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

    suspend fun computeFeedbackForTalk(
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
                    selectedByCurrentUser = feedbackNotes.isNotEmpty()
                )
            }
            .sortedBy { it.first.sort }
    }

    suspend fun computeUserCommentForTalk(
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

    suspend fun computeCommentsForTalk(
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

    suspend fun saveFeedbackState(
        adminNonEncryptedUserEmail: String,
        feedbackId: String,
        type: CommentType,
        accepted: Boolean = true
    ) {
        val feedback = userFeedbackService.findOneOrNull(feedbackId) ?: throw NotFoundException()
        val comment = feedback.comment ?: throw NotFoundException()
        val admin = userService.findOneByNonEncryptedEmailOrNull(adminNonEncryptedUserEmail) ?: throw NotFoundException()

        val newComment = comment.copy(
            approvedInstant = if (accepted) Instant.now() else null,
            approvedByLogin = if (accepted) admin.login else null,
            disapprovedInstant = if (!accepted) Instant.now() else null,
            disapprovedByLogin = if (!accepted) admin.login else null
        )

        userFeedbackService.save(feedback.copy(comment = newComment).toUserFeedback())
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

    enum class CommentType { Unvalidated, Validated, Rejected, All }

    suspend fun findComments(type: CommentType, year: String): Map<TalkDto, List<CachedUserFeedback>> =
        userFeedbackService
            .findAll()
            .asSequence()
            .filter { it.talk.event == year }
            .filter {
                when (type) {
                    All -> true
                    Rejected -> (it.comment?.disapprovedByLogin != null)
                    Validated -> (it.comment?.approvedByLogin != null)
                    Unvalidated -> it.comment?.disapprovedByLogin == null && it.comment?.approvedByLogin == null
                }
            }
            .groupBy { it.talk }
            .filter { it.value.mapNotNull { feed -> feed.comment }.isNotEmpty() }
            .map { (talk, comments) ->
                val speakers = userService.findAllByIds(talk.speakerIds).map { it.toUser() }
                val sortedComments = comments.sortedByDescending { it.creationInstant }.map {
                    val user = it.user.copy(email = cryptographer.decrypt(it.user.email))
                    it.copy(user = user)
                }
                CachedTalk(talk, speakers).toDto(Language.FRENCH) to sortedComments
            }
            .toMap()
}
