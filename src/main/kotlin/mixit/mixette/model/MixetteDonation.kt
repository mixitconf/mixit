package mixit.mixette.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class MixetteDonation(
    val year: String,
    // A donor can be an attendee with a ticket
    val ticketNumber: String? = null,
    // Or a donor can be a registered user
    val userLogin: String? = null,
    // But a user has always an email
    val userEmail: String = "",
    // Denormalized to make requests easier
    val username: String = "",
    // organization receives this donation
    val organizationLogin: String = "",
    // Denormalized to make requests easier
    val organizationName: String = "",
    val quantity: Int = 0,
    val addedAt: Instant = Instant.now(),
    @Id val id: String? = null
) {
    init {
        assert(ticketNumber != null || userLogin != null) {
            "A donation must be made by an attendee with a ticket or a registered login"
        }
    }
}
