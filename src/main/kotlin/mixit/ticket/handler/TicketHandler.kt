package mixit.ticket.handler

import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitApplication
import mixit.MixitProperties
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel.GOLD
import mixit.event.model.SponsorshipLevel.LANYARD
import mixit.event.model.SponsorshipLevel.MIXTEEN
import mixit.event.model.SponsorshipLevel.PARTY
import mixit.event.model.SponsorshipLevel.SILVER
import mixit.routes.MustacheI18n
import mixit.routes.MustacheI18n.CREATION_MODE
import mixit.routes.MustacheI18n.MESSAGE
import mixit.routes.MustacheI18n.TICKET
import mixit.routes.MustacheI18n.TICKETS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheI18n.TYPES
import mixit.routes.MustacheTemplate
import mixit.routes.MustacheTemplate.AdminTicket
import mixit.routes.MustacheTemplate.AdminTicketEdit
import mixit.routes.MustacheTemplate.AdminTicketPrint
import mixit.routes.MustacheTemplate.TicketError
import mixit.routes.Routes
import mixit.security.MixitWebFilter
import mixit.security.model.Cryptographer
import mixit.talk.model.TalkService
import mixit.ticket.model.Ticket
import mixit.ticket.model.TicketService
import mixit.ticket.model.TicketType
import mixit.ticket.model.TicketType.*
import mixit.user.handler.UserHandler
import mixit.user.handler.dto.toDto
import mixit.user.model.Role
import mixit.user.model.UserService
import mixit.util.*
import mixit.util.errors.NotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.renderAndAwait
import reactor.core.publisher.Mono
import java.time.Instant

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
            "/ticket/${ticketNumber}"
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
