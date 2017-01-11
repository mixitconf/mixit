package mixit.controller

import mixit.support.RouterFunctionDsl
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok


class GlobalController : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = RouterFunctionDsl {
        (GET("/") and accept(APPLICATION_JSON)) { indexView() }
        (GET("/sample") and accept(APPLICATION_JSON)) { sampleView() }
        resources("/**", ClassPathResource("static/"))
    } (request)

    fun indexView() = HandlerFunction { ok().render("index") }

    fun sampleView() = HandlerFunction { ok().render("sample") }
}

