package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class SessionSpeaker(
        @Id var id: String,
        var firstname: String,
        var lastname: String
)
