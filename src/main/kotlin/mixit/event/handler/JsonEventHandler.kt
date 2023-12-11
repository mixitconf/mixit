package mixit.event.handler

import mixit.event.repository.EventImagesRepository
import mixit.event.repository.EventRepository
import org.springframework.web.reactive.function.server.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class JsonEventHandler(
    private val repository: EventRepository,
    private val eventImagesRepository: EventImagesRepository
) {

    suspend fun findOne(req: ServerRequest): ServerResponse =
        repository.findOne(req.pathVariable("id")).let { ok().json().bodyValueAndAwait(it) }

    suspend fun findAll(req: ServerRequest): ServerResponse =
        repository.findAll().let { ok().json().bodyValueAndAwait(it) }

    suspend fun findByEventID(req: ServerRequest): ServerResponse =
        repository.findByYear(req.pathVariable("year").toInt()).let { ok().json().bodyValueAndAwait(it) }

    suspend fun findEventImages(req: ServerRequest): ServerResponse =
        eventImagesRepository.findAll().let { ok().json().bodyValueAndAwait(it) }
}
