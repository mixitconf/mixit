package mixit.controller

import mixit.support.RouterFunctionDsl
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


class GlobalController : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = RouterFunctionDsl {
        GET("/") { indexView() }
        GET("/sample") { sampleView() }
        resources("/**", ClassPathResource("static/"))

    } (request)

    fun indexView() = HandlerFunction { ok().render("index") }

    fun sampleView() = HandlerFunction { ok().render("sample") }
}

