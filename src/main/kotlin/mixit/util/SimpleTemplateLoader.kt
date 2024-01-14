package mixit.util

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitApplication
import mixit.event.model.EventService
import mixit.util.mustache.MustacheI18n
import mixit.util.mustache.MustacheTemplate
import mixit.user.model.UserService
import org.springframework.web.reactive.function.server.ServerResponse

object SimpleTemplateLoader {
    suspend fun openTemplate(
        eventService: EventService,
        userService: UserService,
        template: MustacheTemplate,
        params: Map<String, Any> = emptyMap()
    ): ServerResponse {
        val event = eventService.findByYear(MixitApplication.CURRENT_EVENT)

        return ServerResponse.ok()
            .render(
                template.template,
                mapOf(
                    MustacheI18n.TITLE to template.title,
                    MustacheI18n.YEAR to event.year,
                    MustacheI18n.SPONSORS to userService.loadSponsors(event)
                ) + params
            )
            .awaitSingle()
    }
}
