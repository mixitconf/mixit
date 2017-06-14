package mixit.repository

import mixit.model.*
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.stereotype.Repository


@Repository
class TicketRepository(val template: ReactiveMongoTemplate) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun save(ticket: Ticket) =
            template.insert(ticket).doOnSuccess { _ -> logger.info("Save new ticket $ticket") }

    fun findAll() = template.findAll<Ticket>()

}
