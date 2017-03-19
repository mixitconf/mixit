package mixit.web.handler

import mixit.repository.TicketRepository
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.*


@Component
class AdminHandler(val ticketRepository: TicketRepository) {

    fun admin(req: ServerRequest) = ok().render("admin", mapOf(Pair("title", "admin.title")))

    fun adminTicketing(req: ServerRequest) = ticketRepository.findAll().collectList().then { t ->
        ok().render("admin-ticketing", mapOf(Pair("tickets", t), Pair("title", "admin.ticketing.title")))
    }

}


