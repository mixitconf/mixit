package mixit.web.handler.ticketing

import mixit.model.Ticket

class TicketDto(
    val rank: Int,
    val email: String,
    val firstname: String,
    val lastname: String
)

fun Ticket.toDto(rank: Int) = TicketDto(rank, email, firstname, lastname)
