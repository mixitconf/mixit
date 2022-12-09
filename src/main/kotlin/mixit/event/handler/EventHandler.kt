package mixit.event.handler

import mixit.event.model.EventService
import mixit.routes.MustacheI18n.EVENTS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate.EventVideo
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class EventHandler(private val service: EventService) {

    suspend fun findEvents(req: ServerRequest): ServerResponse {
        val events = service
            .findAll()
            .sortedByDescending { it.year }
            .filterNot { it.current }
            .map { it.toEventDto(req.language()) }

        return ok().renderAndAwait(
            EventVideo.template,
            mapOf(
                TITLE to EventVideo.title,
                EVENTS to events
            )
        )
    }

}
