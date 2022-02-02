package mixit.user.model

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
    val links: List<Link>
) {
    constructor(user: User) : this(
        user.login,
        user.firstname,
        user.lastname,
        user.company,
        user.photoUrl,
        user.emailHash,
        user.description.mapValues { it.value.toHTML() },
        user.links
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