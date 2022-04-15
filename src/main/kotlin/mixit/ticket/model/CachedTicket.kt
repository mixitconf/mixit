package mixit.ticket.model

import mixit.security.model.Cryptographer
import mixit.ticket.handler.TicketDto
import mixit.util.cache.Cached
import org.springframework.data.annotation.Id
import java.time.Instant

data class CachedTicket(
    @Id
    val number: String,
    val type: TicketType,
    val encryptedEmail: String,
    val firstname: String? = null,
    val lastname: String,
    val lotteryRank: Int? = null,
    val login: String?,
    val createdAt: Instant = Instant.now()
) : Cached {
    constructor(ticket: Ticket) : this(
        ticket.number,
        ticket.type,
        ticket.encryptedEmail,
        ticket.firstname,
        ticket.lastname,
        ticket.lotteryRank,
        ticket.login,
        ticket.createdAt
    )

    override val id: String
        get() = number

    fun toEntity() = Ticket(
        number = number,
        encryptedEmail = encryptedEmail,
        firstname = firstname,
        lastname = lastname,
        lotteryRank = lotteryRank,
        login = login,
        createdAt = createdAt,
        type = type
    )

    fun toDto(cryptographer: Cryptographer) = TicketDto(
        number = number,
        email = cryptographer.decrypt(encryptedEmail)!!,
        firstname = firstname,
        lastname = lastname,
        lotteryRank = lotteryRank,
        login = login,
        createdAt = createdAt,
        type = type
    )
}
