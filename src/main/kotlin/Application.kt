import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.http.codec.BodyInserters.fromObject
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunctions
import org.springframework.web.reactive.function.RouterFunctions.resources
import org.springframework.web.reactive.function.RouterFunctions.route
import org.springframework.web.reactive.function.ServerResponse.ok
import java.util.concurrent.CompletableFuture

fun main(args: Array<String>) {
    val httpHandler = RouterFunctions.toHttpHandler(routes())
    val server = TomcatServer(httpHandler)
    server.start()

    val stop = CompletableFuture<Void>()
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
        stop.complete(null)
    })
    stop.get()
}

class User(var id:Long, var name:String) {

    override fun toString(): String {
        return "User(id='$id', name='$name')"
    }
}

private fun routes(): RouterFunction<*> {
    return route(GET("/"), HandlerFunction { ok().body(fromObject("Hello Mix-IT!")) })
            .andRoute(GET("/user/{id}"), HandlerFunction { req -> ok().body(fromObject(User(req.pathVariable("id").toLong(), "Robert"))) })
            .and(resources("/**", ClassPathResource("static/")))
}