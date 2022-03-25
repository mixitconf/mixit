package mixit.user.model

import mixit.talk.model.Language
import mixit.user.handler.UserDto
import mixit.user.handler.logoType
import mixit.user.handler.logoWebpUrl
import mixit.util.toHTML

data class CachedStaff(
    val login: String,
    val firstname: String,
    val lastname: String,
    val photoUrl: String?,
    val emailHash: String?,
    val description: Map<Language, String>,
    val links: List<Link>,
    val role: Role
) {
    constructor(user: User) : this(
        user.login,
        user.firstname,
        user.lastname,
        user.photoUrl,
        user.emailHash,
        user.description.mapValues { it.value.toHTML() },
        user.links,
        user.role
    )

    fun toDto(language: Language) =
        UserDto(
            login,
            firstname,
            lastname,
            null,
            "",
            description[language]?.toHTML() ?: "",
            emailHash,
            photoUrl,
            role,
            links,
            logoType(photoUrl),
            logoWebpUrl(photoUrl)
        )
}
