package mixit.ticket.handler

import mixit.MixitProperties
import mixit.security.model.Cryptographer
import mixit.ticket.model.LotteryTicket
import mixit.ticket.repository.LotteryRepository
import mixit.user.model.User
import mixit.util.camelCase
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

    companion object {
        const val TEMPLATE_SUB = "lottery-submission"
        const val TEMPLATE_EDIT = "lottery"
        const val TEMPLATE_CLOSE = "lottery-closed"
        const val TEMPLATE_ERROR = "lottery-error"
    }

    fun findAll(req: ServerRequest) = ok().json().body(ticketRepository.findAll())

    fun ticketing(req: ServerRequest) =
        ok().render(
            if (properties.feature.lottery) TEMPLATE_EDIT else TEMPLATE_CLOSE,
            mapOf(Pair("title", "ticketing.title"))
        )

    fun submit(req: ServerRequest) =
        req.extractFormData().flatMap { formData ->

            val ticket = LotteryTicket(
                formData["email"]!!.lowercase(),
                formData["firstname"]!!,
                formData["lastname"]!!
            )

            // Data are encrypted
            ticketRepository.save(
                ticket.copy(
                    email = cryptographer.encrypt(ticket.email)!!,
                    firstname = cryptographer.encrypt(ticket.firstname.camelCase())!!,
                    lastname = cryptographer.encrypt(ticket.lastname.camelCase())!!
                )
            )
                .then(sendUserConfirmation(ticket, formData, req.locale()))
                .onErrorResume(DuplicateKeyException::class.java) {
                    ok().render(
                        TEMPLATE_ERROR,
                        mapOf(Pair("message", "ticketing.error.alreadyexists"), Pair("title", "ticketing.title"))
                    )
                }
                .onErrorResume {
                    ok().render(
                        TEMPLATE_ERROR,
                        mapOf(Pair("message", "ticketing.error.default"), Pair("title", "ticketing.title"))
                    )
                }
        }

    private fun sendUserConfirmation(
        ticket: LotteryTicket,
        formData: Map<String, String?>,
        locale: Locale
    ): Mono<ServerResponse> {
        val user = User(ticket.email, ticket.firstname, ticket.lastname, cryptographer.encrypt(ticket.email))
        emailService.send("email-ticketing", user, locale)
        return ok().render(TEMPLATE_SUB, formData)
    }
}
