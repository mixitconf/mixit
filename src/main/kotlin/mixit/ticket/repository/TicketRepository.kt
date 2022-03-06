package mixit.ticket.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.ticket.model.Ticket
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository

@Repository
class TicketRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val usersResource = ClassPathResource("data/ticket.json")
            val tickets: List<Ticket> = objectMapper.readValue(usersResource.inputStream)
            tickets.forEach { save(it).block() }
            logger.info("Lottery data initialization complete")
        }
    }

    fun count() = template.count<Ticket>()

    fun save(ticket: Ticket) =
        template.insert(ticket).doOnSuccess { _ -> logger.info("Save new lottery ticket $ticket") }

    fun findAll() = template.findAll<Ticket>().doOnComplete { logger.info("Load all lottery tickets")  }


    fun deleteAll() = template.remove<Ticket>(Query())

    fun deleteOne(id: String) = template.remove<Ticket>(Query(Criteria.where("_id").isEqualTo(id)))

    fun findByEmail(email: String) = template.findOne<Ticket>(Query(Criteria.where("email").isEqualTo(email)))
}
