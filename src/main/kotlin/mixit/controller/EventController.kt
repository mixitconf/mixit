package mixit.controller

import mixit.repository.EventRepository
import mixit.support.RouterFunctionProvider
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Controller
class EventController(val repository: EventRepository) : RouterFunctionProvider() {

    // TODO Remove this@EventController when KT-15667 will be fixed
    override val routes: Routes = {
        (accept(APPLICATION_JSON) and "/api/event").route {
            GET("/", this@EventController::findAll)
            GET("/{login}", this@EventController::findOne)
        }
    }

    fun findOne(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findOne(req.pathVariable("login"))))

    fun findAll(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findAll()))

}
