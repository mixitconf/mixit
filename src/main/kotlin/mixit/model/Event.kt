package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document
data class Event(
    @Id val id: String,
    val start: LocalDate,
    val end: LocalDate,
    val current: Boolean = false,
    val sponsors: List<EventSponsoring> = emptyList(),
    val organizations: List<EventOrganization> = emptyList(),
    val photoUrls: List<Link> = emptyList(),
    val videoUrl: Link? = null,
    val year: Int = start.year
)

@Document
data class EventOrganization(
    val organizationLogin: String,
)

@Document
data class EventSponsoring(
    val level: SponsorshipLevel,
    val sponsorId: String,
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
    ECOLOGY,
    VIDEO,
    COMMUNITY,
    MIXTEEN,
    ECOCUP,
    ACCESSIBILITY,
    NONE
}
