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
    val externalId: String?,
    val type: TicketType,
    val createdAt: Instant
) {
    constructor(ticket: Ticket, cryptographer: Cryptographer) : this(
        number = ticket.number.isNotBlank().let { cryptographer.decrypt(ticket.number)!! },
        email = ticket.encryptedEmail.isNotBlank().let { cryptographer.decrypt(ticket.encryptedEmail)!! },
        firstname = ticket.firstname?.isNotBlank().let { cryptographer.decrypt(ticket.firstname) },
        lastname = ticket.lastname.isNotBlank().let { cryptographer.decrypt(ticket.lastname)!! },
        lotteryRank = ticket.lotteryRank,
        login = ticket.login?.isNotBlank().let { cryptographer.decrypt(ticket.login) },
        externalId = ticket.externalId?.isNotBlank().let { cryptographer.decrypt(ticket.externalId) },
        createdAt = ticket.createdAt,
        type = ticket.type
    )
}
