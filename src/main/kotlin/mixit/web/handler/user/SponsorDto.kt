package mixit.web.handler.user

import java.time.LocalDate
import mixit.model.EventSponsoring
import mixit.model.Language
import mixit.model.SponsorshipLevel
import mixit.model.User
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