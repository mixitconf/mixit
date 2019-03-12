package mixit.web.handler

import mixit.model.Ticket
import mixit.model.User
import mixit.repository.TicketRepository
import mixit.util.*
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import reactor.core.publisher.onErrorResume
import reactor.core.publisher.toMono
import java.util.*

@Component
class TicketingHandler(private val ticketRepository: TicketRepository,
                       private val cryptographer: Cryptographer,
                       private val emailService: EmailService) {

    fun findAll(req: ServerRequest) = ok().json().body(ticketRepository.findAll())

    fun randomDraw(req: ServerRequest) =
            ok().json().body(
                    ticketRepository
                            .findAll()
                            .collectList()
                            .flatMap { it.shuffled(Random())
                                         .distinctBy { listOf(it.firstname, it.lastname) }
                                         .mapIndexed { index, ticket ->  ticket.toDto(index + 1)}
                                         .toMono()
                            })

    fun ticketing(req: ServerRequest) = ServerResponse.ok().render("ticketing", mapOf(Pair("title", "ticketing.title")))

    fun submit(req: ServerRequest) = req.body(BodyExtractors.toFormData()).flatMap {
        val formData  = it.toSingleValueMap()

        val ticket = Ticket(formData["email"]!!,
                formData["firstname"]!!,
                formData["lastname"]!!)

        ticketRepository.save(ticket)
                .then(sendUserConfirmation(ticket, formData, req.locale()))
                .onErrorResume(DuplicateKeyException::class, { ok().render("ticketing-error", mapOf(Pair("message", "ticketing.error.alreadyexists"), Pair("title", "ticketing.title"))) } )
                .onErrorResume { ok().render("ticketing-error", mapOf(Pair("message", "ticketing.error.default"), Pair("title", "ticketing.title"))) }
    }

    private fun sendUserConfirmation(ticket:Ticket, formData: Map<String, String>, locale: Locale): Mono<ServerResponse> {
        val user = User(ticket.email, ticket.firstname, ticket.lastname, cryptographer.encrypt(ticket.email))
        emailService.send("email-ticketing", user, locale)
        return ok().render("ticketing-submission", formData)
    }

    class TicketDto(
            val rank: Int,
            val email: String,
            val firstname: String,
            val lastname: String
    )

    fun Ticket.toDto(rank: Int) = TicketDto(rank, email, firstname, lastname)

}
