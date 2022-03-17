package mixit.util

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class AdminHandler {

    fun admin(req: ServerRequest) =
        ServerResponse.ok().render("admin", mapOf(Pair("title", "admin.title")))
}
