package mixit.ticket.handler

import mixit.ticket.model.Ticket

class TicketDto(
    val rank: Int,
    val email: String,
    val firstname: String,
    val lastname: String
)

fun Ticket.toDto(rank: Int) = TicketDto(rank, email, firstname, lastname)
