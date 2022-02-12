package mixit.ticket.handler

import java.time.Instant
import mixit.MixitProperties
import mixit.ticket.model.FinalTicket
import mixit.ticket.repository.FinalTicketRepository
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

@Component
class AdminTicketHandler(
    private val repository: FinalTicketRepository,
    private val properties: MixitProperties
) {

    companion object {
        const val TEMPLATE_LIST = "admin-ticket"
        const val TEMPLATE_EDIT = "admin-ticket-edit"
        const val LIST_URI = "/admin/ticket"
    }

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun ticketing(req: ServerRequest) =
        ok().render(
            if (properties.feature.lotteryResult) TEMPLATE_LIST else throw NotFoundException(),
            mapOf(
                Pair("title", "admin.ticket.title"),
                Pair("tickets", repository.findAll().map { it.toDto() })
            )
        )

    fun createTicket(req: ServerRequest): Mono<ServerResponse> =
        this.adminTicket()

    fun editTicket(req: ServerRequest): Mono<ServerResponse> =
        repository.findOne(req.pathVariable("number")).flatMap { this.adminTicket(it) }

    private fun adminTicket(ticket: FinalTicket = FinalTicket("","","","")) =
        ok().render(
            if (properties.feature.lotteryResult) TEMPLATE_EDIT else throw NotFoundException(),
            mapOf(
                Pair("creationMode", ticket.email.isEmpty()),
                Pair("ticket", ticket)
            )
        )

    fun submit(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->

            val ticket = FinalTicket(
                formData["email"]!!.lowercase(),
                formData["number"]!!,
                formData["firstname"]!!,
                formData["lastname"]!!,
                Instant.parse(formData["createdAt"])!!
            )

            repository.save(ticket)
                .then(seeOther("${properties.baseUri}${LIST_URI}"))
                .onErrorResume(DuplicateKeyException::class.java) {
                    ok().render(
                        "ticket-error",
                        mapOf(Pair("message", "ticket.error.alreadyexists"), Pair("title", "admin.ticket.title"))
                    )
                }
        }

    fun deleteTicket(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            repository
                .deleteOne(formData["email"]!!)
                .then(seeOther("${properties.baseUri}${LIST_URI}"))
        }
}
