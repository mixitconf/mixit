import org.springframework.web.reactive.function.RouterFunction
import org.springframework.http.codec.BodyInserters
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunctions
import org.springframework.web.reactive.function.RouterFunctions.route
import org.springframework.web.reactive.function.ServerResponse.ok
import reactor.ipc.netty.http.server.HttpServer


fun main(args: Array<String>) {
    val httpHandler = RouterFunctions.toHttpHandler(routes())
    HttpServer.create("0.0.0.0")
            .newHandler(ReactorHttpHandlerAdapter(httpHandler))
            .doOnNext({ foo -> println("Server listening on " + foo.address()) })
            .block()
            .onClose()
            .block()
}

private fun routes(): RouterFunction<*> {
    return route(GET("/"),
            HandlerFunction { request -> ok().body(BodyInserters.fromObject("Hello Mix-IT!")) })
}