package mixit.ticket.handler

import mixit.MixitProperties
import mixit.security.MixitWebFilter
import mixit.security.model.Cryptographer
import mixit.ticket.model.Ticket
import mixit.ticket.model.TicketService
import mixit.ticket.model.TicketType
import mixit.user.model.Role
import mixit.util.enumMatcher
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.json
import mixit.util.seeOther
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Instant

@Component
class AdminTicketHandler(
    private val service: TicketService,
    private val properties: MixitProperties,
    private val cryptographer: Cryptographer
) {

    companion object {
        const val TEMPLATE_LIST = "admin-ticket"
        const val TEMPLATE_PRINT = "admin-ticket-print"
        const val TEMPLATE_EDIT = "admin-ticket-edit"
        const val TEMPLATE_ERROR = "ticket-error"
        const val LIST_URI = "/admin/ticket"
    }

    fun findAll(req: ServerRequest) = ok().json().body(service.findAll().map { tickets ->
        tickets.map { it.toEntity(cryptographer) }
    })

    fun ticketing(req: ServerRequest) =
        ok().render(
            if (properties.feature.lotteryResult) TEMPLATE_LIST else throw NotFoundException(),
            mapOf(
                Pair("title", "admin.ticket.title"),
                Pair("tickets", service.findAll().map { tickets ->
                    tickets.map { it.toDto(cryptographer) }
                })
            )
        )

    fun printTicketing(req: ServerRequest) =
        ok().render(
            if (properties.feature.lotteryResult) TEMPLATE_PRINT else throw NotFoundException(),
            mapOf(
                Pair("title", "admin.ticket.title"),
                Pair("tickets", service.findAll().map { tickets ->
                    tickets.map { ticket ->
                        ticket.toDto(cryptographer)
                            .copy(
                                firstname = ticket.firstname?.uppercase() ?: ticket.lastname.uppercase(),
                                lastname = if (ticket.firstname == null) "" else ticket.lastname.uppercase()
                            )
                    }
                })
            )
        )

    fun createTicket(req: ServerRequest): Mono<ServerResponse> =
        this.adminTicket()

    fun editTicket(req: ServerRequest): Mono<ServerResponse> =
        service.findOne(req.pathVariable("number")).flatMap { this.adminTicket(it.toEntity(cryptographer)) }

    private fun adminTicket(ticket: Ticket = Ticket(cryptographer.encrypt(Ticket.generateNewNumber())!!, "", TicketType.ATTENDEE, "", "")) =
        ok().render(
            if (properties.feature.lotteryResult) TEMPLATE_EDIT else throw NotFoundException(),
            mapOf(
                Pair("creationMode", ticket.encryptedEmail.isEmpty()),
                Pair("ticket", TicketDto(ticket, cryptographer)),
                Pair("types", enumMatcher(ticket) { ticket.type })
            )
        )

    fun submit(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            service.findByNumber(formData["number"]!!)
                .map {
                    it.toEntity(cryptographer).copy(
                        encryptedEmail = cryptographer.encrypt(formData["email"]!!.lowercase())!!,
                        firstname = cryptographer.encrypt(formData["firstname"]),
                        lastname = cryptographer.encrypt(formData["lastname"])!!,
                        externalId = cryptographer.encrypt(formData["externalId"]),
                        type = TicketType.valueOf(formData["type"]!!),
                    )
                }
                .switchIfEmpty(
                    Mono.just(
                        Ticket(
                            number = cryptographer.encrypt(formData["number"])!!,
                            encryptedEmail = cryptographer.encrypt(formData["email"]!!.lowercase())!!,
                            firstname = cryptographer.encrypt(formData["firstname"]),
                            lastname = cryptographer.encrypt(formData["lastname"])!!,
                            externalId = cryptographer.encrypt(formData["externalId"]),
                            lotteryRank = formData["lotteryRank"]?.toInt(),
                            createdAt = Instant.parse(formData["createdAt"])!!,
                            type = TicketType.valueOf(formData["type"]!!),
                        )
                    )
                )
                .flatMap { ticket ->
                    service.save(ticket)
                        .then(seeOther("${properties.baseUri}$LIST_URI"))
                        .onErrorResume(DuplicateKeyException::class.java) {
                            ok().render(
                                TEMPLATE_ERROR,
                                mapOf(
                                    Pair("message", "admin.ticket.error.alreadyexists"),
                                    Pair("title", "admin.ticket.title")
                                )
                            )
                        }
                }
        }

    fun adminDeleteTicket(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            service
                .deleteOne(formData["number"]!!)
                .then(seeOther("${properties.baseUri}$LIST_URI"))
        }

    fun showAttendee(req: ServerRequest): Mono<ServerResponse> =
        service.findByNumber(req.pathVariable("number"))
            .flatMap { attendee ->
                req.session()
                    .flatMap { session ->
                        when (session.getAttribute<Role>(MixitWebFilter.SESSION_ROLE_KEY)) {
                            Role.STAFF -> {
                                // A staff member is redirected to Mixette form
                                seeOther("${properties.baseUri}/admin/mixette-donation/create/${attendee.number}")
                            }
                            Role.VOLUNTEER -> {
                                // A staff member is redirected to Mixette form
                                seeOther("${properties.baseUri}/volunteer/mixette-donation/create/${attendee.number}")
                            }
                            else -> {
                                // Other members could be redirected to user profile in the future
                                seeOther("${properties.baseUri}/")
                            }
                        }
                    }
                    .switchIfEmpty {
                        // Other members are redirected to user view
                        seeOther("${properties.baseUri}/")
                    }
            }
}
