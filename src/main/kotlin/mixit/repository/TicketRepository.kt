package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Role
import mixit.model.Talk
import mixit.model.Ticket
import mixit.model.User
import mixit.util.Cryptographer
import mixit.util.encodeToMd5
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux


@Repository
class TicketRepository(private val template: ReactiveMongoTemplate,
                       private val objectMapper: ObjectMapper) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val usersResource = ClassPathResource("data/ticket.json")
            val tickets: List<Ticket> = objectMapper.readValue(usersResource.inputStream)
            tickets.forEach { save(it).block() }
            logger.info("Ticket data initialization complete")
        }
    }

    fun count() = template.count<Ticket>()

    fun save(ticket: Ticket) =
            template.insert(ticket).doOnSuccess { _ -> logger.info("Save new ticket $ticket") }

    fun findAll() = template.findAll<Ticket>()

    fun deleteAll() = template.remove<Ticket>(Query())

    fun deleteOne(id: String) = template.remove<Ticket>(Query(Criteria.where("_id").isEqualTo(id)))

    fun findByEmail(email: String) = template.findOne<Ticket>(Query(Criteria.where("email").isEqualTo(email)))

}
