package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document
data class Event(
        val year: Int,
        val start: LocalDate,
        val end: LocalDate,
        val current: Boolean = false,
        var sponsors: Iterable<EventSponsoring> = emptyList(),
        @Id val id: String? = null
)