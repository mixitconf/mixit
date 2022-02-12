package mixit.ticket.model

import java.time.Instant
import mixit.ticket.handler.FinalTicketDto
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Should be renamed when the lottery will be closed
 */
@Document
data class Ticket(
    @Id val email: String,
    val firstname: String,
    val lastname: String
)

/**
 * Should be renamed when the lottery will be closed
 */
@Document
data class FinalTicket(
    @Id val email: String,
    val number: String,
    val firstname: String,
    val lastname: String,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun of(ticket: Ticket, rank: Int) =
            FinalTicket(ticket.email, "MXT22-$rank", ticket.firstname, ticket.lastname)
    }

    fun toDto() = FinalTicketDto(email, number, firstname, lastname, createdAt)
}