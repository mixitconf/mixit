package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document
data class EventSponsoring(
        val level: SponsorshipLevel,
        val sponsor: Sponsor,
        val value: LocalDate? = null,
        @Id var id: String? = null
)
