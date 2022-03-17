package mixit.ticket.handler

import mixit.security.model.Cryptographer
import mixit.ticket.model.FinalTicket
import java.time.Instant

class FinalTicketDto(
    val email: String,
    val number: String,
    val firstname: String?,
    val lastname: String,
    val lotteryRank: Int?,
    val login: String?,
    val createdAt: Instant
) {
    constructor(ticket: FinalTicket, cryptographer: Cryptographer) : this(
        number = ticket.number,
        email = cryptographer.decrypt(ticket.encryptedEmail)!!,
        firstname = ticket.firstname,
        lastname = ticket.lastname,
        lotteryRank = ticket.lotteryRank,
        login = ticket.login,
        createdAt = ticket.createdAt
    )
}
