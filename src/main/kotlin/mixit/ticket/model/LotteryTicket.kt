package mixit.ticket.model

import java.time.Instant
import java.util.UUID
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.security.model.Cryptographer
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Should be renamed when the lottery will be closed
 */
@Document
data class LotteryTicket(
    @Id val email: String,
    val firstname: String,
    val lastname: String,
    val rank: Int? = null,
    val interests: List<String> = emptyList(),
)

/**
 * Should be renamed when the lottery will be closed
 */
@Document
data class Ticket(
    @Id val number: String,
    val encryptedEmail: String,
    val type: TicketType,
    val pronoun: TicketPronoun? = null,
    val firstname: String? = null,
    val lastname: String,
    val lotteryRank: Int? = null,
    val externalId: String? = null,
    val login: String? = null,
    val englishSpeaker: Boolean = false,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        fun empty(cryptographer: Cryptographer) =
            Ticket(cryptographer.encrypt(generateNewNumber())!!, "", TicketType.ATTENDEE, null, "", "")

        fun generateNewNumber(): String =
            "MXT$CURRENT_EVENT-${UUID.randomUUID().toString().substring(0, 14).replace("-", "")}"
    }

    fun decrypt(cryptographer: Cryptographer) =
        Ticket(
            number = cryptographer.decrypt(number)!!,
            encryptedEmail = cryptographer.decrypt(encryptedEmail)!!,
            firstname = cryptographer.decrypt(firstname),
            lastname = cryptographer.decrypt(lastname)!!,
            lotteryRank = lotteryRank,
            login = cryptographer.decrypt(login),
            externalId = cryptographer.decrypt(externalId),
            englishSpeaker = englishSpeaker,
            createdAt = createdAt,
            type = type
        )
}
