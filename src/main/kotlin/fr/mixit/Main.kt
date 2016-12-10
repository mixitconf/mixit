package fr.mixit

import java.util.concurrent.CompletableFuture

fun main(args: Array<String>) {

    val application = Application()
    application.start()

    val stop = CompletableFuture<Void>()
    Runtime.getRuntime().addShutdownHook(Thread {
        application.stop()
        stop.complete(null)
    })
    stop.get()
}
