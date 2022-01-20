package mixit.web.handler.mailing

import mixit.model.User

data class MailingDto(
    val title: String,
    val content: String,
    val users: List<User> = emptyList()
)