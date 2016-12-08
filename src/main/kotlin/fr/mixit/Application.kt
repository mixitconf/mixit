package fr.mixit

import fr.mixit.support.TomcatServer
import org.springframework.web.reactive.function.RouterFunctions
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