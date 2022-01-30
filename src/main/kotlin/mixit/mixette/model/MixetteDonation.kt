package mixit.mixette.model

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class MixetteDonation(
    val year: String,
    val userLogin: String = "",
    val username: String = "",
    val organizationLogin: String = "",
    val organizationName: String = "",
    val quantity: Int = 0,
    val addedAt: Instant = Instant.now(),
    @Id val id: String? = null
)
