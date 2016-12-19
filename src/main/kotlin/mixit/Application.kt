package mixit

import com.github.jknack.handlebars.springreactive.HandlebarsViewResolver
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import mixit.controller.GlobalController
import mixit.controller.UserController
import mixit.repository.UserRepository
import mixit.support.*
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener
import java.util.concurrent.CompletableFuture
import org.springframework.context.support.GenericApplicationContext
import java.util.function.Supplier


class Application {

    val context: GenericApplicationContext
    val server: Server
    val hostname: String
    val port: Int?

    constructor(hostname: String = "0.0.0.0", port: Int? = null) {
        this.hostname = hostname
        this.port = port
        context = GenericApplicationContext()
        val env = context.environment
        env.addPropertySource("application.properties")
        val mongoUri = env.getProperty("mongo.uri")
        val mongoDatabase = mongoUri.split("/")[3]

        context.registerBean(IfEqHelperSource::class)
        context.registerBean("messageSource", Supplier {
            val messageSource = ResourceBundleMessageSource()
            messageSource.setBasename("messages")
            messageSource
        })
        context.registerBean(Supplier {
            val viewResolver = HandlebarsViewResolver()
            viewResolver.setPrefix("/templates/")
            viewResolver
        })
        context.registerBean(LoggingEventListener::class)
        context.registerBean(Supplier { MongoClients.create(mongoUri) })
        context.registerBean(Supplier { ReactiveMongoTemplate(context.getBean(MongoClient::class), mongoDatabase) })
        context.registerBean(UserRepository::class)
        context.registerBean(UserController::class)
        context.registerBean(GlobalController::class)
        context.registerBean(Supplier { ReactorNettyServer(hostname, port ?: env.getProperty("server.port").toInt()) })
        context.refresh()

        server = context.getBean(Server::class)
        val userRepository = context.getBean(UserRepository::class)
        userRepository.init()
    }

    fun start() {
        server.start()
    }

    fun await() {
        val stop = CompletableFuture<Void>()
        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
            stop.complete(null)
        })
        stop.get()
    }

    fun stop() {
        context.destroy()
        server.stop()
    }

}