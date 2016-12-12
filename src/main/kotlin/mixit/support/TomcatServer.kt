package mixit.support

import com.github.jknack.handlebars.springreactive.HandlebarsViewResolver
import java.io.File

import org.apache.catalina.LifecycleException
import org.apache.catalina.startup.Tomcat
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

import org.springframework.http.server.reactive.ServletHttpHandlerAdapter
import org.springframework.web.reactive.function.HandlerStrategies
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.web.reactive.function.RouterFunctions
import org.springframework.web.server.adapter.WebHttpHandlerBuilder



class TomcatServer(hostname: String = "localhost", port: Int = 8080) : Server, ApplicationContextAware, InitializingBean {

    lateinit var appContext: ApplicationContext

    override val isRunning: Boolean
        get() = this._isRunning

    private val server: Tomcat
    private var _isRunning: Boolean = false

    init {
        server = Tomcat()
        server.setHostname(hostname)
        server.setPort(port)
    }

    override fun setApplicationContext(context: ApplicationContext) {
        appContext = context
    }

    override fun afterPropertiesSet() {
        val base = File(System.getProperty("java.io.tmpdir"))
        val rootContext = server.addContext("", base.absolutePath)
        val controllers = appContext.getBeansOfType(RouterFunction::class.java).values
        val viewResolver = appContext.getBean(HandlebarsViewResolver::class.java)
        val router = controllers.reduce(RouterFunction<*>::and)
        val strategies = HandlerStrategies.builder().viewResolver(viewResolver).build()
        val webHandler = RouterFunctions.toHttpHandler(router, strategies)
        val httpHandler = WebHttpHandlerBuilder.webHandler(webHandler).filters(LocaleWebFilter()).build()
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