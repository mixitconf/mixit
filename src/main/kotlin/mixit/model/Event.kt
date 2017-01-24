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
    PARTY_DRINKS,
    BREAKFAST,
    LUNCH,
    HOSTING,
    VIDEO,
    COMMUNITY,
    NONE
}