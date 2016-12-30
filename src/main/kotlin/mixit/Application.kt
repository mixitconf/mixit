package mixit

import com.mongodb.ConnectionString
import com.samskivert.mustache.Mustache
import mixit.controller.EventController
import mixit.controller.GlobalController
import mixit.controller.UserController
import mixit.repository.EventRepository
import mixit.repository.SponsorRepository
import mixit.repository.UserRepository
import mixit.support.*
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.web.reactive.result.view.mustache.MustacheResourceTemplateLoader
import org.springframework.web.reactive.result.view.mustache.MustacheViewResolver
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier
import org.springframework.context.support.GenericApplicationContextExtension.registerBean
import org.springframework.beans.factory.BeanFactoryExtension.getBean

class Application {

    val context = AnnotationConfigApplicationContext()
    val server: Server
    val hostname: String
    val port: Int?

    constructor(hostname: String = "0.0.0.0", port: Int? = null) {
        this.hostname = hostname
        this.port = port
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
        context.registerBean(SponsorRepository::class)

        context.registerBean(UserController::class)
        context.registerBean(EventController::class)
        context.registerBean(GlobalController::class)

        context.refresh()
        server = context.getBean(Server::class)
        context.getBean(UserRepository::class).initData()
        context.getBean(EventRepository::class).initData()
        context.getBean(SponsorRepository::class).initData()
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