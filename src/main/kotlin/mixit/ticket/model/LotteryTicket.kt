package mixit.ticket.model

import mixit.event.handler.AdminEventHandler.Companion.CURRENT_EVENT
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

/**
 * Should be renamed when the lottery will be closed
 */
@Document
data class LotteryTicket(
    @Id val email: String,
    val firstname: String,
    val lastname: String,
    val rank: Int? = null
)

/**
 * Should be renamed when the lottery will be closed
 */
@Document
data class Ticket(
    @Id val number: String,
    val encryptedEmail: String,
    val type: TicketType,
    val firstname: String? = null,
    val lastname: String,
    val lotteryRank: Int? = null,
    val externalId: String? = null,
    val login: String? = null,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun generateNewNumber(): String =
            "MXT$CURRENT_EVENT-${UUID.randomUUID().toString().substring(0, 14).replace("-", "")}"
    }
}
