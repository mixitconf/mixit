package mixit.web.handler

import mixit.model.Ticket
import mixit.repository.TicketRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.onErrorResume

@Component
class TicketingHandler(val repository: TicketRepository) {

    fun ticketing(req: ServerRequest) = ServerResponse.ok().render("ticketing-closed", mapOf(Pair("title", "ticketing.title")))

    fun submit(req: ServerRequest) = req.body(BodyExtractors.toFormData()).flatMap {
        val formData  = it.toSingleValueMap()
        val ticket = Ticket(formData["email"]!!,
                formData["firstname"]!!,
                formData["lastname"]!!)
        repository.save(ticket)
                .then(ok().render("ticketing-submission", formData))
                .onErrorResume(DuplicateKeyException::class, { ok().render("ticketing-error", mapOf(Pair("message", "ticketing.error.alreadyexists"), Pair("title", "ticketing.title"))) } )
                .onErrorResume { ok().render("ticketing-error", mapOf(Pair("message", "ticketing.error.default"), Pair("title", "ticketing.title"))) }
    }
}
