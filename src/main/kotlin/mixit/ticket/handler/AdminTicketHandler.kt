package mixit.ticket.handler

import mixit.MixitProperties
import mixit.ticket.model.FinalTicket
import mixit.ticket.model.TicketService
import mixit.user.model.Role
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
    private val properties: MixitProperties
) {

    companion object {
        const val TEMPLATE_LIST = "admin-ticket"
        const val TEMPLATE_EDIT = "admin-ticket-edit"
        const val LIST_URI = "/admin/ticket"
    }

    fun findAll(req: ServerRequest) = ok().json().body(service.findAll())

    fun ticketing(req: ServerRequest) =
        ok().render(
            if (properties.feature.lotteryResult) TEMPLATE_LIST else throw NotFoundException(),
            mapOf(
                Pair("title", "admin.ticket.title"),
                Pair("tickets", service.findAll().map { it.toDto() })
            )
        )

    fun createTicket(req: ServerRequest): Mono<ServerResponse> =
        this.adminTicket()

    fun editTicket(req: ServerRequest): Mono<ServerResponse> =
        service.findOne(req.pathVariable("number")).flatMap { this.adminTicket(it.toEntity()) }

    private fun adminTicket(ticket: FinalTicket = FinalTicket(FinalTicket.generateNewNumber(), "", "", "")) =
        ok().render(
            if (properties.feature.lotteryResult) TEMPLATE_EDIT else throw NotFoundException(),
            mapOf(
                Pair("creationMode", ticket.email.isEmpty()),
                Pair("ticket", ticket)
            )
        )

    fun submit(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            service.findByNumber(formData["number"]!!)
                .map {
                    it.toEntity().copy(
                        email = formData["email"]!!.lowercase(),
                        firstname = formData["firstname"]!!,
                        lastname = formData["lastname"]!!,
                    )
                }
                .switchIfEmpty(
                    Mono.just(
                        FinalTicket(
                            number = formData["number"]!!,
                            email = formData["email"]!!.lowercase(),
                            firstname = formData["firstname"]!!,
                            lastname = formData["lastname"]!!,
                            lotteryRank = formData["lotteryRank"]?.toInt(),
                            createdAt = Instant.parse(formData["createdAt"])!!
                        )
                    )
                )
                .flatMap { ticket ->
                    service.save(ticket)
                        .then(seeOther("${properties.baseUri}${LIST_URI}"))
                        .onErrorResume(DuplicateKeyException::class.java) {
                            ok().render(
                                "ticket-error",
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
                .then(seeOther("${properties.baseUri}${LIST_URI}"))
        }

    fun showAttendee(req: ServerRequest): Mono<ServerResponse> =
            service.findByNumber(req.pathVariable("number"))
                .flatMap { attendee ->
                    req.session()
                        .flatMap { session ->
                            val role = session.getAttribute<Role>("role")
                            if (role == Role.STAFF || role == Role.VOLUNTEER) {
                                // A staff member is redirected to Mixette form
                                seeOther("${properties.baseUri}/mixette-donation/create/${attendee.number}")
                            }
                            else {
                                // Other members could be redirected to user profile in the future
                                seeOther("${properties.baseUri}/")
                            }
                        }
                        .switchIfEmpty {
                            // Other members are redirected to user view
                            seeOther("${properties.baseUri}/")
                        }
                }
}
