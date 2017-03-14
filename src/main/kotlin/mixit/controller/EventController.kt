package mixit.controller

import mixit.repository.EventRepository
import mixit.util.json
import mixit.util.router
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Controller
class EventController(val repository: EventRepository) {

    @Bean
    fun eventRouter() = router {
        (accept(APPLICATION_JSON) and "/api/event").route {
            GET("/", this@EventController::findAll)
            GET("/{login}", this@EventController::findOne)
        }
    }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

}

