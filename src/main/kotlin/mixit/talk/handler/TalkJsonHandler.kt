package mixit.talk.handler

import mixit.talk.model.TalkService
import mixit.util.json
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class TalkJsonHandler(private val service: TalkService) {

    suspend fun findOne(req: ServerRequest) =
        service
            .findOneOrNull(req.pathVariable("id"))
            ?.sanitizeForApi()
            ?.let { ok().json().bodyValueAndAwait(it) }
            ?: throw NotFoundException()

    suspend fun findByEventId(req: ServerRequest) =
        service
            .findByEvent(req.pathVariable("year"))
            .map { it.sanitizeForApi() }
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun findAdminByEventId(req: ServerRequest) =
        service
            .findByEvent(req.pathVariable("year"))
            .let { ok().json().bodyValueAndAwait(it) }
}
