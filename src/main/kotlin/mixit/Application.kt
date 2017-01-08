package mixit

import com.mongodb.ConnectionString
import com.samskivert.mustache.Mustache
import mixit.controller.*
import mixit.repository.*
import mixit.support.ReactorNettyServer
import mixit.support.Server
import mixit.support.addPropertySource
import org.springframework.beans.factory.BeanFactoryExtension.getBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.GenericApplicationContextExtension.registerBean
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.web.reactive.result.view.mustache.MustacheResourceTemplateLoader
import org.springframework.web.reactive.result.view.mustache.MustacheViewResolver
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

class Application {

    val hostname: String
    val port: Int?

    var context: AnnotationConfigApplicationContext? = null
    var server: Server? = null

    constructor(port: Int? = null, hostname: String = "0.0.0.0") {
        this.hostname = hostname
        this.port = port
    }

    fun start() {
        var context = AnnotationConfigApplicationContext()
        val env = context.environment
        env.addPropertySource("application.properties")
        val mongoUri = env.getProperty("mongo.uri")

        context.registerBean("messageSource", Supplier {
            val messageSource = ReloadableResourceBundleMessageSource()
            messageSource.setBasename("messages")
            messageSource.setDefaultEncoding("UTF-8")
            messageSource
        })
        context.registerBean(Supplier {
            val prefix = "classpath:/templates/"
            val suffix = ".mustache"
            val loader = MustacheResourceTemplateLoader(prefix, suffix)
            val resolver = MustacheViewResolver()
            resolver.setPrefix(prefix)
            resolver.setSuffix(suffix)
            resolver.setCompiler(Mustache.compiler().withLoader(loader))
            resolver
        })
        context.registerBean(Supplier {
            ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(ConnectionString(mongoUri)))
        })
        context.registerBean(Supplier{
            ReactiveMongoRepositoryFactory(context.getBean(ReactiveMongoTemplate::class))
        })
        context.registerBean(Supplier { ReactorNettyServer(hostname, port ?: env.getProperty("server.port").toInt()) })

        context.registerBean(UserRepository::class)
        context.registerBean(EventRepository::class)
        context.registerBean(SessionRepository::class)

        context.registerBean(UserController::class)
        context.registerBean(EventController::class)
        context.registerBean(SessionController::class)
        context.registerBean(GlobalController::class)

        context.refresh()
        val server = context.getBean(Server::class)
        context.getBean(UserRepository::class).initData()
        context.getBean(EventRepository::class).initData()
        context.getBean(SessionRepository::class).initData()
        server.start()

        this.context = context
        this.server = server
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
        server?.stop()
        context?.destroy()
    }

}

fun main(args: Array<String>) {
    val application = Application()
    application.start()
    application.await()
}
