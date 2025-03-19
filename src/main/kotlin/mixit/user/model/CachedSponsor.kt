package mixit.user.model

import mixit.event.model.EventSponsoring
import mixit.event.model.SponsorshipLevel
import mixit.talk.model.Language
import mixit.user.handler.dto.EventSponsoringDto
import mixit.user.handler.dto.toSponsorDto
import mixit.util.toHTML
import java.time.LocalDate

data class CachedSponsor(
    val login: String,
    val company: String,
    val photoUrl: String?,
    val photoShape: PhotoShape? = null,
    val description: Map<Language, String>,
    val links: List<Link>,
    val level: SponsorshipLevel,
    val subscriptionDate: LocalDate,
    val email: String? = null
) {
    constructor(user: User, sponsoring: EventSponsoring) : this(
        user.login,
        user.company ?: "${user.lastname} ${user.firstname}",
        user.photoUrl,
        user.photoShape,
        user.description.mapValues { it.value.toHTML() },
        user.links.filterNot { it.isTwitter() },
        sponsoring.level,
        sponsoring.subscriptionDate,
        user.email
    )

    fun toEventSponsoringDto(language: Language) =
        EventSponsoringDto(
            level,
            toSponsorDto(language),
            subscriptionDate
        )
}
