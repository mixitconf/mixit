package mixit

import com.mongodb.ConnectionString
import com.mongodb.DBRef
import com.samskivert.mustache.Mustache
import mixit.controller.GlobalController
import mixit.controller.UserController
import mixit.repository.UserRepository
import mixit.support.*
import org.bson.Document
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.convert.Jsr310Converters
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.web.reactive.result.view.mustache.MustacheResourceTemplateLoader
import org.springframework.web.reactive.result.view.mustache.MustacheViewResolver
import org.springframework.data.mongodb.core.convert.*
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier


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
            val messageSource = ResourceBundleMessageSource()
            messageSource.setBasename("messages")
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
            val factory = SimpleReactiveMongoDatabaseFactory(ConnectionString(mongoUri))
            val conversions = CustomConversions(Jsr310Converters.getConvertersToRegister().toMutableList())
            val mappingContext = MongoMappingContext()
            mappingContext.setSimpleTypeHolder(conversions.simpleTypeHolder)
            mappingContext.initialize()
            val mappingConverter = MappingMongoConverter(NoOpDbRefResolver(), mappingContext)
            mappingConverter.setCustomConversions(conversions)
            ReactiveMongoTemplate(factory, mappingConverter)
        })
        context.registerBean(Supplier{
            ReactiveMongoRepositoryFactory(context.getBean(ReactiveMongoTemplate::class))
        })
        context.registerBean(UserRepository::class)
        context.registerBean(UserController::class)
        context.registerBean(GlobalController::class)
        context.registerBean(Supplier { ReactorNettyServer(hostname, port ?: env.getProperty("server.port").toInt()) })
        context.refresh()

        server = context.getBean(Server::class)
        val userRepository = context.getBean(UserRepository::class)
        userRepository.initData()
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

    // TODO to be removed when we can, Spring Data MongoDB should not require that
    inner class NoOpDbRefResolver : DbRefResolver {

        override fun resolveDbRef(property: MongoPersistentProperty, dbref: DBRef, callback: DbRefResolverCallback, proxyHandler: DbRefProxyHandler?): Any? {
            return null
        }

        override fun createDbRef(annotation: org.springframework.data.mongodb.core.mapping.DBRef?, entity: MongoPersistentEntity<*>?, id: Any?): DBRef? {
            return null
        }

        override fun fetch(dbRef: DBRef): Document? {
            return null
        }

        override fun bulkFetch(dbRefs: MutableList<DBRef>?): MutableList<Document>? {
            return null
        }
    }

}