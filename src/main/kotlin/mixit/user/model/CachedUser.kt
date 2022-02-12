package mixit.user.model

import mixit.talk.model.Language
import mixit.user.handler.UserDto
import mixit.user.handler.logoType
import mixit.user.handler.logoWebpUrl
import mixit.util.Cached
import mixit.util.toHTML

data class CachedUser(
        val login: String,
        val firstname: String,
        val lastname: String,
        val company: String?,
        val email: String?,
        val photoUrl: String?,
        val emailHash: String?,
        val description: Map<Language, String>,
        val links: List<Link>,
        val legacyId: Long? = null,
        val role: Role,
        override val id: String = login
) : Cached {
    constructor(user: User) : this(
            user.login,
            user.firstname,
            user.lastname,
            user.company,
            user.email,
            user.photoUrl,
            user.emailHash,
            user.description,
            user.links,
            user.legacyId,
            user.role
    )

    fun toDto(language: Language) =
            UserDto(
                    login,
                    firstname,
                    lastname,
                    null,
                    company,
                    description[language]?.toHTML() ?: "",
                    emailHash,
                    photoUrl,
                    role,
                    links,
                    logoType(photoUrl),
                    logoWebpUrl(photoUrl)
            )

    fun toUser() =
            User(
                    login,
                    firstname,
                    lastname,
                    email,
                    company,
                    description,
                    emailHash,
                    photoUrl,
                    role,
                    links,
                    legacyId
            )

}