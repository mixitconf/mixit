package mixit.repository

import mixit.model.*
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Repository


@Repository
class TicketRepository(val template: ReactiveMongoTemplate) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun save(ticket: Ticket) =
            template.insert(ticket).doOnSuccess { t -> logger.info("Save new ticket $ticket") }


}
