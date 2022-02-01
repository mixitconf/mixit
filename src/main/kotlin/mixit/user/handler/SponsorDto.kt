package mixit.user.handler

import java.time.LocalDate
import mixit.event.model.SponsorshipLevel
import mixit.talk.model.Language
import mixit.user.cache.CachedSponsor
import mixit.user.model.Link
import mixit.util.toUrlPath

data class EventSponsoringDto(
    val level: SponsorshipLevel,
    val sponsor: SponsorDto,
    val subscriptionDate: LocalDate = LocalDate.now()
)

data class SponsorDto(
    val login: String,
    var company: String,
    var photoUrl: String,
    val logoType: String?,
    val logoWebpUrl: String? = null,
    var description: String? = null,
    var links: List<Link> = emptyList(),
    val isAbsoluteLogo: Boolean = photoUrl.startsWith("http"),
    val path:String = login.toUrlPath()
)

fun CachedSponsor.toSponsorDto(language: Language = Language.FRENCH) =
    SponsorDto(
        login,
        company,
        photoUrl ?: "unknown",
        logoType(photoUrl),
        logoWebpUrl(photoUrl),
        description[language],
        links
    )