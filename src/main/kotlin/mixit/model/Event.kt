package mixit.model

import mixit.util.MarkdownConverter
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document
data class Event(
        @Id val id: String,
        val start: LocalDate,
        val end: LocalDate,
        val current: Boolean = false,
        val sponsors: List<EventSponsoring> = emptyList()
) {
    val year: Int = start.year
}

@Document
data class EventSponsoring(
        val level: SponsorshipLevel,
        val sponsor: User,
        val subscriptionDate: LocalDate = LocalDate.now()
)

enum class SponsorshipLevel {
    GOLD,
    SILVER,
    BRONZE,
    LANYARD,
    PARTY,
    BREAKFAST,
    LUNCH,
    HOSTING,
    VIDEO,
    COMMUNITY,
    NONE
}

class SponsorDto(
    val login: String,
    val company: String,
    val logoUrl: String,
    val logoType: String,
    val logoWebpUrl: String? = null
)

fun EventSponsoring.toDto() = SponsorDto(
    this.sponsor.login,
    this.sponsor.company!!,
    this.sponsor.logoUrl!!,
    logoType(this.sponsor.logoUrl),
    logoWebpUrl(this.sponsor.logoUrl)
)

private fun logoWebpUrl(url: String) =
        when {
            url.endsWith("png") -> url.replace("png", "webp")
            url.endsWith("jpg") -> url.replace("jpg", "webp")
            else -> null
        }

private fun logoType(url: String) =
        when {
            url.endsWith("svg") -> "image/svg+xml"
            url.endsWith("png") -> "image/png"
            url.endsWith("jpg") -> "image/jpeg"
            else -> throw IllegalArgumentException("Extension not supported")
        }

class EventSponsoringDto(
    val level: SponsorshipLevel,
    val sponsor: UserDto,
    val subscriptionDate: LocalDate = LocalDate.now()
)

fun EventSponsoring.toDto(language: Language, markdownConverter: MarkdownConverter) =
        EventSponsoringDto(level, sponsor.toDto(language, markdownConverter), subscriptionDate)
