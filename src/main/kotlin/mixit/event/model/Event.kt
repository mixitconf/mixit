package mixit.event.model

import java.time.LocalDate
import mixit.user.model.Link
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Event(
    @Id val id: String = "",
    val start: LocalDate = LocalDate.now(),
    val end: LocalDate = LocalDate.now(),
    val current: Boolean = false,
    val sponsors: List<EventSponsoring> = emptyList(),
    val organizations: List<EventOrganization> = emptyList(),
    val volunteers: List<EventVolunteer> = emptyList(),
    val organizers: List<EventOrganizer> = emptyList(),
    val photoUrls: List<Link> = emptyList(),
    val videoUrl: Link? = null,
    val schedulingFileUrl: String? = null,
    val year: Int = start.year,
    val streamingUrl: String? = null
)

@Document
data class EventOrganization(
    val organizationLogin: String
)

data class EventSponsoring(
    val level: SponsorshipLevel = SponsorshipLevel.NONE,
    val sponsorId: String = "",
    val subscriptionDate: LocalDate = LocalDate.now()
)

data class EventVolunteer(
    val volunteerLogin: String
)

data class EventOrganizer(
    val organizerLogin: String
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
    NONE;

    companion object {
        fun sponsorshipLevels() =
            listOf(GOLD, SILVER, HOSTING, ECOLOGY, LANYARD, ACCESSIBILITY, MIXTEEN, PARTY, VIDEO)
    }
}
