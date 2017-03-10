package mixit.controller

import mixit.model.Ticket
import mixit.repository.TicketRepository
import mixit.util.RouterFunctionProvider
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.Routes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.*

//@Controller
class AttendController(val repository: TicketRepository) : RouterFunctionProvider() {

    override val routes: Routes = {
        accept(MediaType.TEXT_HTML).route {
            GET("/attend", this@AttendController::attendView)
        }
        contentType(MediaType.APPLICATION_FORM_URLENCODED).route {
            POST("/attend", this@AttendController::submit)
        }
    }

    fun attendView(req: ServerRequest) = ok().render("attend")

    fun submit(req: ServerRequest) = req.body(BodyExtractors.toFormData()).then { data ->
        val map  = data.toSingleValueMap()
        val ticket = Ticket(map["email"]!!,
                map["firstname"]!!,
                map["lastname"]!!)
        repository.save(ticket)
                .then { t -> ok().render("attend-submission") }
                .otherwise(DuplicateKeyException::class.java, { ok().render("attend-error", mapOf(Pair("message", "attend.error.alreadyexists"))) } )
                .otherwise { ok().render("attend-error", mapOf(Pair("message", "attend.error.default"))) }
    }
}
