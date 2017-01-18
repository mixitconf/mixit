package mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok

@Controller
class GlobalController : RouterFunction<ServerResponse> {

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/") { indexView() }
        }
        resources("/**", ClassPathResource("static/"))
    }

    fun indexView() = ok().render("index")
}
