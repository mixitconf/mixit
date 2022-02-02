package mixit.user.model

import mixit.talk.model.Language
import mixit.util.toHTML

data class CachedOrganization(
    val login: String,
    val company: String,
    val photoUrl: String?,
    val description: Map<Language, String>,
    val links: List<Link>,
) {
    constructor(user: User): this(
        user.login,
        user.company ?: "${user.lastname} ${user.firstname}",
        user.photoUrl,
        user.description.mapValues {it.value.toHTML() },
        user.links
    )
}