package mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok


class GlobalController : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = route(request) {
        accept(TEXT_HTML).apply {
            GET("/") { indexView() }
        }
        resources("/**", ClassPathResource("static/"))
    }

    fun indexView() = HandlerFunction { ok().render("index") }
}
