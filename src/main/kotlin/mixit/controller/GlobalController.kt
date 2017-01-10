package mixit.controller

import mixit.support.invoke
import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok


class GlobalController : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) =
        "/" {
                        GET { indexView() }
            "**" {      resources(ClassPathResource("static/")) }
            "sample" {  GET { sampleView() } }
        } (request)

    fun indexView() = HandlerFunction { ok().render("index") }

    fun sampleView() = HandlerFunction { ok().render("sample") }
}

