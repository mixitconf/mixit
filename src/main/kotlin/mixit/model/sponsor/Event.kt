package mixit.model.sponsor

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

// TODO Switch to val + noarg compiler plugin when it will be stable in IDEA
@Document
data class Event(
        @Id var id: Long = 0,
        var year: Int = 0,
        var current: Boolean=false,
        var start: Instant = Instant.MIN,
        var end: Instant = Instant.MIN
)
