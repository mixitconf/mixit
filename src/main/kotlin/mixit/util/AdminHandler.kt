package mixit.util

import kotlinx.coroutines.reactor.awaitSingle
import mixit.routes.MustacheI18n
import mixit.routes.MustacheTemplate.Admin
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class AdminHandler {

    suspend fun admin(req: ServerRequest): ServerResponse =
        ServerResponse
            .ok()
            .render(Admin.template, MustacheI18n.TITLE to "admin.title")
            .awaitSingle()
}
