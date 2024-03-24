package mixit.ticket.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.ticket.model.Ticket
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

@Repository
class TicketRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        //deleteAll().block()
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
