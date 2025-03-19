package mixit.user.model

import mixit.talk.model.Language
import mixit.user.handler.dto.SponsorDto
import mixit.user.handler.dto.toLinkDto
import mixit.user.handler.logoType
import mixit.user.handler.logoWebpUrl
import mixit.util.toHTML

data class CachedOrganization(
    val login: String,
    val company: String,
    val photoUrl: String?,
    val photoShape: PhotoShape? = null,
    val description: Map<Language, String>,
    val links: List<Link>,
    val email: String? = null
) {
    constructor(user: User) : this(
        user.login,
        user.company ?: "${user.lastname} ${user.firstname}",
        user.photoUrl,
        user.photoShape,
        user.description.mapValues { it.value.toHTML() },
        user.links.filterNot { it.isTwitter() },
        user.email
    )

    fun toSponsorDto(language: Language) = SponsorDto(
        login,
        company,
        photoUrl ?: "unknown",
        photoShape == PhotoShape.Rectangle,
        logoType(photoUrl),
        logoWebpUrl(photoUrl),
        description[language],
        links.mapIndexed { index, link -> link.toLinkDto(index) }
    )
}
