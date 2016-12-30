package mixit.model.sponsor

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

// TODO Switch to val + noarg compiler plugin when it will be stable in IDEA
@Document
data class EventSponsoring(
        @Id var id: Long = 0,
        var event: Event = Event(),
        var level: SponsorshipLevel = SponsorshipLevel.NONE,
        val sponsor: Sponsor = Sponsor()
)
