package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class EventSponsoring(
        val event: Event,
        val level: SponsorshipLevel,
        val sponsor: Sponsor,
        @Id var id: String? = null
)
