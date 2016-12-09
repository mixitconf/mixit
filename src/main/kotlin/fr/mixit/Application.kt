package fr.mixit

import fr.mixit.controller.Controllers
import fr.mixit.support.TomcatServer
import org.springframework.web.reactive.function.RouterFunctions

fun main(args: Array<String>) {
    val httpHandler = RouterFunctions.toHttpHandler(Controllers.routes())
    val server = TomcatServer(httpHandler)
    server.startAndAwait()
}