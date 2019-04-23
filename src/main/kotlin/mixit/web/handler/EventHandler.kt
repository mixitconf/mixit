package mixit.web.handler

import mixit.repository.EventRepository
import mixit.util.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Component
class EventHandler(private val repository: EventRepository) {

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("id")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun findByEventID(req: ServerRequest) = ok().json().body(repository.findByYear(req.pathVariable("year").toInt()))

}

