package mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.web.reactive.function.RouterFunctions.resources
import org.springframework.web.reactive.function.ServerRequest
import org.springframework.web.reactive.function.ServerResponse.status
import java.net.URI
import java.util.*

class ResourceController : RouterFunction<Any> {

    // Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) =
        resources("/**", ClassPathResource("static/"))
                .andRoute(GET("/"), HandlerFunction { status(HttpStatus.SEE_OTHER).location(URI.create("/index.html")).build() })
                .route(request) as Optional<HandlerFunction<Any>>

}

