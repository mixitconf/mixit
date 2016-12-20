package mixit.support

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.core.codec.CharSequenceEncoder
import org.springframework.core.codec.StringDecoder
import org.springframework.http.codec.DecoderHttpMessageReader
import org.springframework.http.codec.EncoderHttpMessageWriter
import org.springframework.http.codec.ResourceHttpMessageWriter
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.result.view.ViewResolver

import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.ipc.netty.NettyContext
import reactor.ipc.netty.http.server.HttpServer
import java.util.concurrent.atomic.AtomicReference

class ReactorNettyServer(hostname: String, port: Int) : Server, ApplicationContextAware, InitializingBean {

    override val isRunning: Boolean
        get() {
            val context = this.nettyContext.get()
            return context != null && context.channel().isActive
        }

    private val server = HttpServer.create(hostname, port)
    private val nettyContext = AtomicReference<NettyContext>()
    lateinit private var appContext: ApplicationContext
    lateinit private var reactorHandler: ReactorHttpHandlerAdapter

    override fun setApplicationContext(context: ApplicationContext) {
        appContext = context
    }

    override fun afterPropertiesSet() {
        val controllers = appContext.getBeansOfType(RouterFunction::class).values
        val viewResolver = appContext.getBean(ViewResolver::class)
        val router = controllers.reduce(RouterFunction<*>::and)
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build()
        val strategies = HandlerStrategies.empty()
                .viewResolver(viewResolver)
                .messageReader(DecoderHttpMessageReader(StringDecoder()))
                .messageReader(DecoderHttpMessageReader(Jackson2JsonDecoder(objectMapper)))
                .messageWriter(EncoderHttpMessageWriter(CharSequenceEncoder()))
                .messageWriter(ResourceHttpMessageWriter())
                .messageWriter(EncoderHttpMessageWriter(Jackson2JsonEncoder(objectMapper)))
                .build()
        val webHandler = RouterFunctions.toHttpHandler(router, strategies)
        val httpHandler = WebHttpHandlerBuilder.webHandler(webHandler).filters(MixitWebFilter()).build()
        reactorHandler = ReactorHttpHandlerAdapter(httpHandler)
    }

    override fun start() {
        if (!this.isRunning) {
            if (this.nettyContext.get() == null) {
                this.nettyContext.set(server.newHandler(reactorHandler)
                        .doOnNext { println("Reactor Netty server started on ${it.address()}") }
                        .block())
            }
        }
    }

    override fun stop() {
        if (this.isRunning) {
            val context = this.nettyContext.getAndSet(null)
            context?.dispose()
        }
    }

}