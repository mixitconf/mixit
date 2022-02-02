package mixit.talk.handler

import mixit.talk.model.TalkService
import mixit.util.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body

@Component
class JsonTalkHandler(private val service: TalkService) {

    fun findOne(req: ServerRequest) =
        ok().json().body(service.findOne(req.pathVariable("id")).map { it.sanitizeForApi() })

    fun findByEventId(req: ServerRequest) =
        ok().json().body(service.findByEvent(req.pathVariable("year"))
            .map { talks -> talks.map { it.sanitizeForApi() } })

    fun findAdminByEventId(req: ServerRequest) =
        ok().json().body(service.findByEvent(req.pathVariable("year")))


}

