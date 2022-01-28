package mixit.ticket.handler

import mixit.MixitProperties
import mixit.ticket.model.Ticket
import mixit.ticket.repository.TicketRepository
import mixit.util.camelCase
import mixit.util.extractFormData
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AdminTicketingHandler(
    private val ticketRepository: TicketRepository,
    private val properties: MixitProperties
) {

    companion object {
        const val TEMPLATE_LIST = "admin-ticketing"
        const val LIST_URI = "/admin/ticketing"
    }

    fun adminTicketing(req: ServerRequest): Mono<ServerResponse> {
        val tickets = ticketRepository.findAll()
            .map {
                Ticket(it.email, it.firstname.camelCase(), it.lastname.camelCase())
            }
            .sort(Comparator.comparing(Ticket::lastname).thenComparing(Comparator.comparing(Ticket::firstname)))
        return ok().render(
            TEMPLATE_LIST,
            mapOf(
                Pair("tickets", tickets),
                Pair("title", "admin.ticketing.title")
            )
        )
    }

    fun adminDeleteTicketing(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            ticketRepository
                .deleteOne(formData["email"]!!)
                .then(seeOther("${properties.baseUri}$LIST_URI"))
        }
}
