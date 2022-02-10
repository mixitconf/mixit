package mixit.ticket.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.ticket.model.FinalTicket
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

@Repository
class FinalTicketRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val usersResource = ClassPathResource("data/final-ticket.json")
            val tickets: List<FinalTicket> = objectMapper.readValue(usersResource.inputStream)
            tickets.forEach { save(it).block() }
            logger.info("Ticket data initialization complete")
        }
    }

    fun count() = template.count<FinalTicket>()

    fun save(ticket: FinalTicket) =
        template.insert(ticket).doOnSuccess { _ -> logger.info("Save new ticket $ticket") }

    fun findAll() =
        template.findAll<FinalTicket>()

    fun findOne(login: String) =
        template.findById<FinalTicket>(login)

    fun deleteAll() =
        template.remove<FinalTicket>(Query())

    fun deleteOne(id: String) =
        template.remove<FinalTicket>(Query(Criteria.where("_id").isEqualTo(id)))

    fun findByEmail(email: String) =
        template.findOne<FinalTicket>(Query(Criteria.where("email").isEqualTo(email)))
}
