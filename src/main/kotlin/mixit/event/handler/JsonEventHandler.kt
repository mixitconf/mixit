package mixit.event.handler

import mixit.event.repository.EventRepository
import mixit.util.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body

@Component
class JsonEventHandler(private val repository: EventRepository) {

    fun findOne(req: ServerRequest) =
        ok().json().body(repository.findOne(req.pathVariable("id")))

    fun findAll(req: ServerRequest) =
        ok().json().body(repository.findAll())

    fun findByEventID(req: ServerRequest) =
        ok().json().body(repository.findByYear(req.pathVariable("year").toInt()))
}
