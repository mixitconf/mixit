package mixit.user.handler

import mixit.event.model.SponsorshipLevel
import mixit.talk.model.Language
import mixit.user.model.CachedSponsor
import mixit.user.model.Link
import mixit.user.model.PhotoShape
import mixit.util.toUrlPath
import java.time.LocalDate

data class EventSponsoringDto(
    val level: SponsorshipLevel,
    val sponsor: SponsorDto,
    val subscriptionDate: LocalDate = LocalDate.now()
)

data class SponsorDto(
    val login: String,
    var company: String,
    var photoUrl: String,
    val isRectangleLogo: Boolean,
    val logoType: String?,
    val logoWebpUrl: String? = null,
    var description: String? = null,
    var links: List<Link> = emptyList(),
    val isAbsoluteLogo: Boolean = photoUrl.startsWith("http"),
    val path: String = login.toUrlPath()
)

fun CachedSponsor.toSponsorDto(language: Language = Language.FRENCH) =
    SponsorDto(
        login,
        company,
        photoUrl ?: "unknown",
        photoShape == PhotoShape.Rectangle,
        logoType(photoUrl),
        logoWebpUrl(photoUrl),
        description[language],
        links
    )
