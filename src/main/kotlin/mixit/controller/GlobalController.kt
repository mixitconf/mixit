package mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.web.reactive.function.RouterFunctions.resources
import org.springframework.web.reactive.function.ServerRequest
import org.springframework.web.reactive.function.ServerResponse
import java.util.*

class GlobalController : RouterFunction<Any> {

    // Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) =
        resources("/**", ClassPathResource("static/"))
                .andRoute(GET("/"), indexView())
                .route(request) as Optional<HandlerFunction<Any>>

    fun indexView() = HandlerFunction { req ->
        ServerResponse.ok().render("index")
    }
}

