package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import java.util.*

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
) {

    companion object {
        fun shuffle(sponsors: Iterable<EventSponsoring>?): Iterable<EventSponsoring>? {
            if (sponsors == null) return null;
            val shuffledSponsors: MutableList<EventSponsoring> = sponsors.toMutableList();
            Collections.shuffle(shuffledSponsors);
            return shuffledSponsors.asIterable();
        }
    }
}

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