package mixit.user.model

import mixit.talk.model.Language
import mixit.user.handler.dto.UserDto
import mixit.user.handler.dto.toLinkDto
import mixit.user.handler.logoType
import mixit.user.handler.logoWebpUrl
import mixit.util.cache.Cached
import mixit.util.toHTML

data class CachedUser(
    val login: String,
    val firstname: String,
    val lastname: String,
    val company: String?,
    var email: String?,
    val photoUrl: String?,
    val photoShape: PhotoShape? = null,
    val emailHash: String?,
    val description: Map<Language, String>,
    val links: List<Link>,
    val legacyId: Long? = null,
    val role: Role,
    val cfpId: String? = null,
    var newsletterSubscriber: Boolean,
    override val id: String = login
) : Cached {
    constructor(user: User) : this(
        user.login,
        user.firstname,
        user.lastname,
        user.company,
        user.email,
        user.photoUrl,
        user.photoShape,
        user.emailHash,
        user.description,
        user.links.filterNot { it.isTwitter() },
        user.legacyId,
        user.role,
        user.cfpId,
        user.newsletterSubscriber
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
            photoShape,
            role,
            links.mapIndexed { index, link -> link.toLinkDto(index) },
            logoType(photoUrl),
            logoWebpUrl(photoUrl),
            cfpId = cfpId,
            newsletterSubscriber = newsletterSubscriber,
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
            photoShape,
            role,
            links,
            legacyId,
            cfpId = cfpId,
            newsletterSubscriber = newsletterSubscriber
        )

    val organizationName
        get() = company ?: "$firstname $lastname"
}
