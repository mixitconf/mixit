package mixit.feedback.model

import java.time.Instant
import mixit.talk.model.Talk
import mixit.user.model.User
import mixit.util.cache.Cached

data class CachedUserFeedback(
    override val id: String,
    val user: User,
    val talk: Talk,
    val event: String,
    val creationInstant: Instant,
    val notes: List<Feedback>,
    val comment: FeedbackComment?
) : Cached {
    constructor(userFeedback: UserFeedback, talk: Talk, user: User) : this(
        userFeedback.id!!,
        user,
        talk,
        userFeedback.event,
        userFeedback.creationInstant,
        userFeedback.notes,
        userFeedback.comment
    )

    fun toUserFeedback() =
        UserFeedback(
            id = this.id,
            notes = notes,
            event = event,
            talkId = talk.id ?: "unknown talk",
            comment = comment,
            encryptedEmail = user.email ?: "unknown email",
            creationInstant = creationInstant,
        )

    fun updateWith(userFeedback: UserFeedback) =
        copy(
            notes = userFeedback.notes,
            comment = userFeedback.comment
        )
}
