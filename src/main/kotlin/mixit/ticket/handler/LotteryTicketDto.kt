package mixit.ticket.handler

import mixit.ticket.model.LotteryTicket

class LotteryTicketDto(
    val rank: Int,
    val email: String,
    val firstname: String,
    val lastname: String
)

fun LotteryTicket.toDto(rank: Int) = LotteryTicketDto(rank, email, firstname, lastname)
