package mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.resources
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono


class GlobalController : RouterFunction<ServerResponse> {

    @Suppress("UNCHECKED_CAST")// TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) =
        resources("/**", ClassPathResource("static/"))
                .andRoute(GET("/"), indexView())
                .andRoute(GET("/sample"), sampleView())
                .route(request) as Mono<HandlerFunction<ServerResponse>>

    fun indexView() = HandlerFunction { ServerResponse.ok().render("index") }

    fun sampleView() = HandlerFunction { ServerResponse.ok().render("sample") }
}

