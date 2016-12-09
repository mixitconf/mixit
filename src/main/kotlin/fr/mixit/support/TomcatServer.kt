package fr.mixit.support

import java.io.File

import org.apache.catalina.LifecycleException
import org.apache.catalina.startup.Tomcat

import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter
import java.util.concurrent.CompletableFuture

class TomcatServer {

    private val server: Tomcat
    private var isRunning: Boolean = false

    constructor(httpHandler: HttpHandler, hostname:String = "localhost", port: Int = 8080) {
        server = Tomcat()
        server.setHostname(hostname)
        server.setPort(port)
        val base = File(System.getProperty("java.io.tmpdir"))
        val rootContext = server.addContext("", base.getAbsolutePath())
        Tomcat.addServlet(rootContext, "httpHandlerServlet", ServletHttpHandlerAdapter(httpHandler))
        rootContext.addServletMappingDecoded("/", "httpHandlerServlet")
    }

    fun start() {
        if (!this.isRunning) {
            try {
                this.isRunning = true
                this.server.start()
            } catch (ex: LifecycleException) {
                throw IllegalStateException(ex)
            }
        }
    }

    fun startAndAwait() {
        start()
        val stop = CompletableFuture<Void>()
        Runtime.getRuntime().addShutdownHook(Thread {
            server.stop()
            stop.complete(null)
        })
        stop.get()
    }

    fun stop() {
        if (this.isRunning) {
            try {
                this.isRunning = false
                this.server.stop()
                this.server.destroy()
            } catch (ex: LifecycleException) {
                throw IllegalStateException(ex)
            }
        }
    }

}