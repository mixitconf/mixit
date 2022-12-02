package mixit.util

import mixit.routes.MustacheI18n
import mixit.routes.MustacheTemplate.Admin
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class AdminHandler {

    suspend fun admin(req: ServerRequest): ServerResponse =
        ServerResponse.ok().renderAndAwait(Admin.template, mapOf(MustacheI18n.TITLE to Admin.title))
}
