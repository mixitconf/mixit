package mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok


class GlobalController : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = RouterFunctionDsl {
        accept(APPLICATION_JSON).apply {
            GET("/") { indexView() }
            GET("/sample") { sampleView() }
        }
        resources("/**", ClassPathResource("static/"))
    } (request)

    fun indexView() = HandlerFunction { ok().render("index") }

    fun sampleView() = HandlerFunction { ok().render("sample") }
}

