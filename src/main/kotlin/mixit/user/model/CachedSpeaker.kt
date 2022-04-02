package mixit.user.model

import mixit.event.handler.AdminEventHandler
import mixit.talk.model.Language
import mixit.util.toHTML

data class CachedSpeaker(
    val login: String,
    val firstname: String,
    val lastname: String,
    val company: String?,
    val photoUrl: String?,
    val emailHash: String?,
    val description: Map<Language, String>,
    val links: List<Link>,
    val year: Int
) {
    constructor(user: User, year: Int = AdminEventHandler.CURRENT_EVENT.toInt()) : this(
        user.login,
        user.firstname,
        user.lastname,
        user.company,
        user.photoUrl,
        user.emailHash,
        user.description.mapValues { it.value.toHTML() },
        user.links,
        year
    )

    fun toUser() = User(
        login,
        firstname,
        lastname,
        null,
        company,
        description,
        emailHash,
        photoUrl,
        links = links
    )
}
