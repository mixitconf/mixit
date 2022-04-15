package mixit.ticket.handler

import mixit.security.model.Cryptographer
import mixit.ticket.model.Ticket
import mixit.ticket.model.TicketType
import java.time.Instant

data class TicketDto(
    val email: String,
    val number: String,
    val firstname: String?,
    val lastname: String,
    val lotteryRank: Int?,
    val login: String?,
    val type: TicketType,
    val createdAt: Instant
) {
    constructor(ticket: Ticket, cryptographer: Cryptographer) : this(
        number = ticket.number,
        email = cryptographer.decrypt(ticket.encryptedEmail)!!,
        firstname = ticket.firstname,
        lastname = ticket.lastname,
        lotteryRank = ticket.lotteryRank,
        login = ticket.login,
        createdAt = ticket.createdAt,
        type = ticket.type
    )
}
