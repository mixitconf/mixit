package mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.web.reactive.function.RouterFunctions.resources
import org.springframework.web.reactive.function.ServerRequest

class ResourceController : RouterFunction<Resource> {

    override fun route(request: ServerRequest) =
        resources("/**", ClassPathResource("static/")).route(request)

}

