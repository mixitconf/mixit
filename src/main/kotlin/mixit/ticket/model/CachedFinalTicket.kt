package mixit.ticket.model

import mixit.security.model.Cryptographer
import mixit.ticket.handler.FinalTicketDto
import mixit.util.cache.Cached
import org.springframework.data.annotation.Id
import java.time.Instant

data class CachedFinalTicket(
    @Id
    val number: String,
    val encryptedEmail: String,
    val firstname: String? = null,
    val lastname: String,
    val lotteryRank: Int? = null,
    val login: String?,
    val createdAt: Instant = Instant.now()
) : Cached {
    constructor(ticket: FinalTicket) : this(
        ticket.number,
        ticket.encryptedEmail,
        ticket.firstname,
        ticket.lastname,
        ticket.lotteryRank,
        ticket.login,
        ticket.createdAt
    )

    override val id: String
        get() = number

    fun toEntity() = FinalTicket(
        number = number,
        encryptedEmail = encryptedEmail,
        firstname = firstname,
        lastname = lastname,
        lotteryRank = lotteryRank,
        login = login,
        createdAt = createdAt
    )

    fun toDto(cryptographer: Cryptographer) = FinalTicketDto(
        number = number,
        email = cryptographer.decrypt(encryptedEmail)!!,
        firstname = firstname,
        lastname = lastname,
        lotteryRank = lotteryRank,
        login = login,
        createdAt = createdAt
    )
}
