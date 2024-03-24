package mixit.ticket.model

import java.time.Instant
import mixit.security.model.Cryptographer
import mixit.ticket.handler.TicketDto
import mixit.util.cache.Cached
import org.springframework.data.annotation.Id

data class CachedTicket(
    @Id
    val number: String,
    val type: TicketType,
    val email: String,
    val pronoun: TicketPronoun? = null,
    val firstname: String? = null,
    val lastname: String,
    val lotteryRank: Int? = null,
    val externalId: String? = null,
    val login: String?,
    val englishSpeaker: Boolean = false,
    val createdAt: Instant = Instant.now()
) : Cached {
    constructor(cryptographer: Cryptographer, ticket: Ticket) : this(
        number = cryptographer.decrypt(ticket.number)!!,
        type = ticket.type,
        email = cryptographer.decrypt(ticket.encryptedEmail)!!,
        pronoun = ticket.pronoun,
        firstname = cryptographer.decrypt(ticket.firstname),
        lastname = cryptographer.decrypt(ticket.lastname)!!,
        lotteryRank = ticket.lotteryRank,
        externalId = cryptographer.decrypt(ticket.externalId),
        login = cryptographer.decrypt(ticket.login),
        englishSpeaker = ticket.englishSpeaker,
        createdAt = ticket.createdAt
    )

    override val id: String
        get() = number

    fun toEntity(cryptographer: Cryptographer) = Ticket(
        number = cryptographer.encrypt(number)!!,
        encryptedEmail = cryptographer.encrypt(email)!!,
        pronoun = pronoun,
        firstname = cryptographer.encrypt(firstname),
        lastname = cryptographer.encrypt(lastname)!!,
        lotteryRank = lotteryRank,
        login = cryptographer.encrypt(login),
        externalId = cryptographer.encrypt(externalId),
        createdAt = createdAt,
        englishSpeaker = englishSpeaker,
        type = type
    )

    fun toDto(cryptographer: Cryptographer) = TicketDto(toEntity(cryptographer), cryptographer)
}
