package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document
data class WorkAdventure(
        @Id val ticket: String,
        val token: String,
        val username: String ?= null
)
