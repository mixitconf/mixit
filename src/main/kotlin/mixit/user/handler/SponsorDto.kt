package mixit.user.handler

import java.time.LocalDate
import mixit.event.model.EventSponsoring
import mixit.event.model.SponsorshipLevel
import mixit.talk.model.Language
import mixit.user.model.User
import mixit.util.MarkdownConverter

data class EventSponsoringDto(
    val level: SponsorshipLevel,
    val sponsor: UserDto,
    val subscriptionDate: LocalDate = LocalDate.now()
)

data class SponsorDto(
    val login: String,
    var company: String,
    var photoUrl: String,
    val logoType: String?,
    val logoWebpUrl: String? = null,
    val isAbsoluteLogo: Boolean = photoUrl.startsWith("http")
)

fun EventSponsoring.toDto(sponsor: User, language: Language, markdownConverter: MarkdownConverter) =
    EventSponsoringDto(level, sponsor.toDto(language, markdownConverter), subscriptionDate)

fun EventSponsoring.toSponsorDto(sponsor: User) =
    SponsorDto(
        sponsor.login,
        sponsor.company!!,
        sponsor.photoUrl!!,
        logoType(sponsor.photoUrl),
        logoWebpUrl(sponsor.photoUrl)
    )