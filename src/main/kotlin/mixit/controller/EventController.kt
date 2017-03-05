package mixit.controller

import mixit.repository.EventRepository
import mixit.util.RouterFunctionProvider
import mixit.util.json
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Controller
class EventController(val repository: EventRepository) : RouterFunctionProvider() {

    override val routes: Routes = {
        (accept(APPLICATION_JSON) and "/api/event").route {
            GET("/", this@EventController::findAll)
            GET("/{login}", this@EventController::findOne)
        }
    }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

}
