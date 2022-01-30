package mixit.mailing.handler

import mixit.user.model.User

data class MailingDto(
    val title: String,
    val content: String,
    val users: List<User> = emptyList()
)