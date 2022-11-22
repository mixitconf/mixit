package mixit.event.handler

import mixit.event.repository.EventRepository
import mixit.util.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class JsonEventHandler(private val repository: EventRepository) {

    suspend fun findOne(req: ServerRequest): ServerResponse =
        repository.coFindOne(req.pathVariable("id")).let { ok().json().bodyValueAndAwait(it) }

    suspend fun findAll(req: ServerRequest): ServerResponse =
        repository.coFindAll().let { ok().json().bodyValueAndAwait(it) }

    suspend fun findByEventID(req: ServerRequest): ServerResponse =
        repository.coFindByYear(req.pathVariable("year").toInt()).let { ok().json().bodyValueAndAwait(it) }
}
