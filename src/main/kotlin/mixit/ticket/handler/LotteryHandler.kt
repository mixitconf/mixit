package mixit.ticket.handler

import mixit.MixitProperties
import mixit.security.model.Cryptographer
import mixit.ticket.model.Ticket
import mixit.ticket.repository.LotteryRepository
import mixit.user.model.User
import mixit.util.email.EmailService
import mixit.util.extractFormData
import mixit.util.json
import mixit.util.locale
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import java.util.Locale

@Component
class LotteryHandler(
    private val ticketRepository: LotteryRepository,
    private val cryptographer: Cryptographer,
    private val emailService: EmailService,
    private val properties: MixitProperties
) {

    fun findAll(req: ServerRequest) = ok().json().body(ticketRepository.findAll())

    fun ticketing(req: ServerRequest) =
        ok().render(
            if (properties.feature.lottery) "ticketing" else "ticketing-closed",
            mapOf(Pair("title", "ticketing.title"))
        )

    fun submit(req: ServerRequest) =
        req.extractFormData().flatMap { formData ->

            val ticket = Ticket(
                formData["email"]!!.lowercase(),
                formData["firstname"]!!,
                formData["lastname"]!!
            )

            ticketRepository.save(ticket)
                .then(sendUserConfirmation(ticket, formData, req.locale()))
                .onErrorResume(DuplicateKeyException::class.java) {
                    ok().render(
                        "ticketing-error",
                        mapOf(Pair("message", "ticketing.error.alreadyexists"), Pair("title", "ticketing.title"))
                    )
                }
                .onErrorResume {
                    ok().render(
                        "ticketing-error",
                        mapOf(Pair("message", "ticketing.error.default"), Pair("title", "ticketing.title"))
                    )
                }
        }

    private fun sendUserConfirmation(
        ticket: Ticket,
        formData: Map<String, String?>,
        locale: Locale
    ): Mono<ServerResponse> {
        val user = User(ticket.email, ticket.firstname, ticket.lastname, cryptographer.encrypt(ticket.email))
        emailService.send("email-ticketing", user, locale)
        return ok().render("ticketing-submission", formData)
    }
}
