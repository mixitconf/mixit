package mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.web.reactive.function.RouterFunctions.resources
import org.springframework.web.reactive.function.ServerRequest
import org.springframework.web.reactive.function.ServerResponse
import reactor.core.publisher.Mono


class GlobalController : RouterFunction<ServerResponse> {

    // TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) =
        resources("/**", ClassPathResource("static/"))
                .andRoute(GET("/"), indexView())
                .andRoute(GET("/sample"), sampleView())
                .route(request) as Mono<HandlerFunction<ServerResponse>>

    fun indexView() = HandlerFunction { ServerResponse.ok().render("index") }

    fun sampleView() = HandlerFunction { ServerResponse.ok().render("sample") }
}

