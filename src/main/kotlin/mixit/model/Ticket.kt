package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document
data class Ticket(
        @Id val email: String,
        val firstname: String,
        val lastname: String
)
