package mixit.ticket.handler

import mixit.util.mustache.MustacheTemplate
import mixit.routes.Routes
import mixit.ticket.model.TicketService
import mixit.ticket.model.TicketType.ATTENDEE
import mixit.ticket.model.TicketType.SPONSOR_LANYARD
import mixit.ticket.model.TicketType.SPONSOR_MIXTEEN
import mixit.ticket.model.TicketType.SPONSOR_PARTNER
import mixit.ticket.model.TicketType.SPONSOR_PARTY
import mixit.ticket.model.TicketType.SPONSOR_STAND
import mixit.user.model.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class TicketHandler(
    private val service: TicketService,
    private val userService: UserService
) {

    companion object {
        val sponsors = listOf(
            SPONSOR_LANYARD,
            SPONSOR_PARTY,
            SPONSOR_MIXTEEN,
            SPONSOR_STAND,
            SPONSOR_PARTNER
        )
    }

    suspend fun viewTicket(uri: String): String {
        val ticketNumber = uri.replace(Routes.mixetteQRCode, "")
        val ticket = service.findByNumber(ticketNumber) ?: return ""
        val user = userService.findOneByNonEncryptedEmailOrNull(ticket.email)

        // When ticket is found we have to redirect the user
        return if (user != null) {
            if (sponsors.contains(ticket.type)) {
                "/sponsor/${user.login}"
            } else {
                "/user/${user.login}"
            }
        } else {
            "/ticket/$ticketNumber"
        }
    }

    suspend fun findOneView(req: ServerRequest): ServerResponse {
        val ticketNumber = req.pathVariable("number")
        val ticket = service.findByNumber(ticketNumber)
        val params = mapOf(
            "firstname" to (ticket?.firstname ?: "Unknown"),
            "lastname" to (ticket?.lastname ?: ""),
            "type" to (ticket?.type ?: ATTENDEE)
        )
        return ok().renderAndAwait(MustacheTemplate.UserTicket.template, params)
    }
}
