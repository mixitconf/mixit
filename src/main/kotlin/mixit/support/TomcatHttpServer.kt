package mixit.support

import java.io.File

import org.apache.catalina.LifecycleException
import org.apache.catalina.startup.Tomcat

import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter

class TomcatHttpServer(httpHandler: HttpHandler) : HttpServer {

    // TODO Allow to customize hostname and port
    val hostname: String = "localhost"
    val port: Int = 8080

    override val isRunning: Boolean
        get() = this._isRunning

    private val server: Tomcat
    private var _isRunning: Boolean = false

    init {
        server = Tomcat()
        server.setHostname(hostname)
        server.setPort(port)
        val base = File(System.getProperty("java.io.tmpdir"))
        val rootContext = server.addContext("", base.absolutePath)
        Tomcat.addServlet(rootContext, "httpHandlerServlet", ServletHttpHandlerAdapter(httpHandler))
        rootContext.addServletMappingDecoded("/", "httpHandlerServlet")
    }

    override fun start() {
        if (!this.isRunning) {
            try {
                this._isRunning = true
                this.server.start()
            } catch (ex: LifecycleException) {
                throw IllegalStateException(ex)
            }
        }
    }

    override fun stop() {
        if (this.isRunning) {
            try {
                this._isRunning = false
                this.server.stop()
                this.server.destroy()
            } catch (ex: LifecycleException) {
                throw IllegalStateException(ex)
            }
        }
    }

}