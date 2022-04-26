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
    val email: String,
    val firstname: String? = null,
    val lastname: String,
    val lotteryRank: Int? = null,
    val externalId: String? = null,
    val login: String?,
    val createdAt: Instant = Instant.now()
) : Cached {
    constructor(cryptographer: Cryptographer, ticket: Ticket) : this(
        cryptographer.decrypt(ticket.number)!!,
        ticket.type,
        cryptographer.decrypt(ticket.encryptedEmail)!!,
        cryptographer.decrypt(ticket.firstname),
        cryptographer.decrypt(ticket.lastname)!!,
        ticket.lotteryRank,
        cryptographer.decrypt(ticket.externalId),
        cryptographer.decrypt(ticket.login),
        ticket.createdAt
    )

    override val id: String
        get() = number

    fun toEntity(cryptographer: Cryptographer) = Ticket(
        number = cryptographer.encrypt(number)!!,
        encryptedEmail = cryptographer.encrypt(email)!!,
        firstname = cryptographer.encrypt(firstname),
        lastname = cryptographer.encrypt(lastname)!!,
        lotteryRank = lotteryRank,
        login = cryptographer.encrypt(login),
        externalId = cryptographer.encrypt(externalId),
        createdAt = createdAt,
        type = type
    )

    fun toDto(cryptographer: Cryptographer) = TicketDto(toEntity(cryptographer), cryptographer)
}
