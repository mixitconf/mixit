package mixit.controller

import mixit.repository.TicketRepository
import mixit.util.RouterFunctionProvider
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.Routes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Controller
class AdminController(val ticketRepository: TicketRepository) : RouterFunctionProvider() {

    override val routes: Routes = {
        GET("/admin") { ServerResponse.ok().render("admin") }
        GET("/admin/ticketing", this@AdminController::adminTicketing)
    }

    fun adminTicketing(req: ServerRequest) = ticketRepository.findAll().collectList().then { t ->
        ServerResponse.ok().render("admin-ticketing", mapOf(Pair("tickets", t)))
    }

}


