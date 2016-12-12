package mixit

import com.github.jknack.handlebars.springreactive.HandlebarsViewResolver
import com.mongodb.reactivestreams.client.MongoClients
import mixit.controller.GlobalController
import mixit.controller.UserController
import mixit.repository.UserRepository
import mixit.support.*
import org.springframework.context.MessageSource
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.mapping.event.LoggingEventListener
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import java.util.concurrent.CompletableFuture


class Application {

    val appContext: AnnotationConfigApplicationContext
    val server: Server

    constructor() {
        appContext = appContext()
        appContext.refresh()
        server = appContext.getBean(Server::class)
        val userRepository = appContext.getBean(UserRepository::class)
        userRepository.init()
    }

    // TODO Replace by function bean registration API when available, see https://jira.spring.io/browse/SPR-14832
    private fun appContext() : AnnotationConfigApplicationContext {
        val appContext = AnnotationConfigApplicationContext()
        appContext.register(ApplicationConfiguration::class)
        appContext.register(UserRepository::class)
        appContext.register(UserController::class)
        appContext.register(GlobalController::class)
        appContext.register(IfEqHelperSource::class)
        // TODO Replace by Reactor Netty as soon as static file handling and flushing are fixed
        appContext.register(TomcatServer::class)
        return appContext
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
        server.stop()
        appContext.destroy()
    }

    // TODO Replace by function bean registration API when available, see https://jira.spring.io/browse/SPR-14832
    @Configuration
    @EnableReactiveMongoRepositories
    open class ApplicationConfiguration : AbstractReactiveMongoConfiguration() {

        @Bean
        override fun mongoClient() = MongoClients.create()

        override fun getDatabaseName() = "mixit"

        @Bean
        open fun mongoEventListener() = LoggingEventListener()

        @Bean
        open fun mongoTemplate() = ReactiveMongoTemplate(mongoClient(), databaseName)

        @Bean
        open fun viewResolver(): HandlebarsViewResolver {
            var viewResolver = HandlebarsViewResolver()
            viewResolver.setPrefix("/templates/")
            return viewResolver
        }

        @Bean
        open fun messageSource(): MessageSource {
            val messageSource = ResourceBundleMessageSource()
            messageSource.setBasename("messages")
            return messageSource
        }

    }


}