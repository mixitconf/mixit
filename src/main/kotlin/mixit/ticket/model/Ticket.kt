package mixit.ticket.model

import mixit.event.handler.AdminEventHandler.Companion.CURRENT_EVENT
import mixit.ticket.handler.FinalTicketDto
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.*

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
    @Id val number: String,
    val email: String,
    val firstname: String,
    val lastname: String,
    val lotteryRank: Int? = null,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun generateNewNumber(): String =
            "MXT$CURRENT_EVENT-${UUID.randomUUID().toString().substring(0, 14).replace("-", "")}"

        fun createFromLottery(lotteryTicket: Ticket, rank: Int?) =
            lotteryTicket.let {
                FinalTicket(it.email, generateNewNumber(), it.firstname, it.lastname, rank)
            }
    }

    fun toDto() = FinalTicketDto(email, number, firstname, lastname, lotteryRank, createdAt)
}