package mixit.controller

import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.web.reactive.function.RouterFunctions

class Controllers(val controllers: List<RouterFunction<*>>) : HttpHandler {

    override fun handle(request: ServerHttpRequest, response: ServerHttpResponse) =
            RouterFunctions.toHttpHandler(controllers.reduce(RouterFunction<*>::and)).handle(request, response)

}
