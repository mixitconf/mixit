package mixit.talk.handler

import mixit.talk.model.TalkService
import mixit.util.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class TalkJsonHandler(private val service: TalkService) {

    suspend fun findOne(req: ServerRequest) =
        service
            .coFindOne(req.pathVariable("id"))
            .sanitizeForApi()
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun findByEventId(req: ServerRequest) =
        service
            .coFindByEvent(req.pathVariable("year"))
            .map { it.sanitizeForApi() }
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun findAdminByEventId(req: ServerRequest) =
        service
            .coFindByEvent(req.pathVariable("year"))
            .let { ok().json().bodyValueAndAwait(it) }
}
