package mixit.ticket.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant
import mixit.security.model.Cryptographer
import mixit.ticket.model.Ticket
import mixit.ticket.model.TicketPronoun
import mixit.ticket.model.TicketType
import mixit.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

data class TicketTemp(
    val number: String?,
    val encryptedEmail: String,
    val type: TicketType,
    val pronoun: String? = null,
    val firstname: String? = null,
    val lastname: String,
    val externalId: String? = null,
    val englishSpeaker: Int = 0,
    val createdAt: Instant = Instant.now()
) {
    fun toTicket(cryptographer: Cryptographer) = Ticket(
        number = cryptographer.encrypt(if(number.isNullOrBlank()) Ticket.generateNewNumber() else number)!!,
        encryptedEmail = cryptographer.encrypt(encryptedEmail)!!,
        type = type,
        pronoun = if (pronoun == null) null
        else if (pronoun.lowercase().startsWith("il")) {
            TicketPronoun.HE_HIM
        } else if (pronoun.lowercase().startsWith("elle")) {
            TicketPronoun.SHE_HER
        } else if (pronoun.lowercase().startsWith("iel")) {
            TicketPronoun.THEY_THEM
        } else if (pronoun.lowercase().startsWith("xe")) {
            TicketPronoun.XE_XEM
        } else if (pronoun.lowercase().startsWith("ask")) {
            TicketPronoun.ASK_ME
        } else if (pronoun.lowercase().startsWith("jeudi")) {
            TicketPronoun.THURSDAY
        } else if (pronoun.lowercase().startsWith("vendredi")) {
            TicketPronoun.FRIDAY
        } else if (pronoun.lowercase().startsWith("full")) {
            TicketPronoun.TWO_DAYS
        } else {
            null
        },
        firstname = cryptographer.encrypt(firstname),
        lastname = cryptographer.encrypt(lastname)!!,
        externalId = cryptographer.encrypt(externalId),
        englishSpeaker = englishSpeaker == 1,
        createdAt = createdAt
    )
}

@Repository
class TicketRepository(
    private val template: ReactiveMongoTemplate,
    private val lotteryRepository: LotteryRepository,
    private val userRepository: UserRepository,
    private val cryptographer: Cryptographer,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
//        deleteAll().block()
//        if (count().block() == 0L) {
//            ClassPathResource("data/import_lottery_speaker.json").inputStream.use { resource ->
//                runBlocking {
//                    val tickets: List<TicketTemp> = objectMapper.readValue(resource)
//                    val users = userRepository.findAll().filterNot { it.email == null }.associateBy { it.email }
//                    val lotteries = lotteryRepository.findAll().associateBy { it.email }
//
//                    tickets
//                        .forEach { ticketTemp ->
//                            val encryptedEmail = cryptographer.encrypt(ticketTemp.encryptedEmail)!!
//                            val ticket = ticketTemp.toTicket(cryptographer)
//                                .copy(
//                                    login = cryptographer.encrypt(users[encryptedEmail]?.login),
//                                    lotteryRank = lotteries[encryptedEmail]?.rank,
//                                )
//                            save(ticket).block()
//                        }
//
//                }
//                logger.info("Ticket data initialization complete")
//            }
//        }
        if (count().block() == 0L) {
            ClassPathResource("data/ticket.json").inputStream.use { resource ->
                val tickets: List<Ticket> = objectMapper.readValue(resource)
                tickets.forEach { save(it).block() }
                logger.info("Ticket data initialization complete")
            }
        }
    }

    fun count() = template.count<Ticket>()

    fun save(ticket: Ticket) =
        template.save(ticket).doOnSuccess { _ -> logger.info("Save new ticket $ticket") }

    fun findAll(): Flux<Ticket> =
        template.findAll<Ticket>().doOnComplete { logger.info("Load all tickets") }

    fun findOne(login: String) =
        template.findById<Ticket>(login)

    fun deleteAll() =
        template.remove<Ticket>(Query())

    fun deleteOne(id: String) =
        template.remove<Ticket>(Query(Criteria.where("_id").isEqualTo(id)))

    fun findByEmail(email: String) =
        template.findOne<Ticket>(Query(Criteria.where("email").isEqualTo(email)))
}
