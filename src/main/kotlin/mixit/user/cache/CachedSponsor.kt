package mixit.user.cache

import java.time.LocalDate
import mixit.event.model.EventSponsoring
import mixit.event.model.SponsorshipLevel
import mixit.talk.model.Language
import mixit.user.model.Link
import mixit.user.model.User
import mixit.util.toHTML

data class CachedSponsor(
    val login: String,
    val company: String,
    val photoUrl: String?,
    val description: Map<Language, String>,
    val links: List<Link>,
    val level: SponsorshipLevel,
    val subscriptionDate: LocalDate
){
    constructor(user: User, sponsoring: EventSponsoring): this(
        user.login,
        user.company ?: "${user.lastname} ${user.firstname}",
        user.photoUrl,
        user.description.mapValues {it.value.toHTML() },
        user.links,
        sponsoring.level,
        sponsoring.subscriptionDate
    )
}