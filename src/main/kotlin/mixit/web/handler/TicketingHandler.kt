package mixit.web.handler

import mixit.model.Ticket
import mixit.repository.TicketRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*

@Component
class TicketingHandler(val repository: TicketRepository) {

    fun ticketing(req: ServerRequest) = ServerResponse.ok().render("ticketing")

    fun submit(req: ServerRequest) = req.body(BodyExtractors.toFormData()).then { data ->
        val formData  = data.toSingleValueMap()
        val ticket = Ticket(formData["email"]!!,
                formData["firstname"]!!,
                formData["lastname"]!!)
        repository.save(ticket)
                .then { t -> ok().render("ticketing-submission", formData) }
                .otherwise(DuplicateKeyException::class.java, { ok().render("ticketing-error", mapOf(Pair("message", "ticketing.error.alreadyexists"))) } )
                .otherwise { ok().render("ticketing-error", mapOf(Pair("message", "ticketing.error.default"))) }
    }
}
