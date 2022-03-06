package mixit.ticket.handler

import java.time.Instant

class FinalTicketDto(
    val email: String,
    val number: String,
    val firstname: String?,
    val lastname: String,
    val lotteryRank: Int?,
    val login: String?,
    val createdAt: Instant
)
