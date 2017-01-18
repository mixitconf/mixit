package mixit

import com.mongodb.ConnectionString
import com.samskivert.mustache.Mustache
import mixit.repository.ArticleRepository
import mixit.repository.EventRepository
import mixit.repository.SessionRepository
import mixit.repository.UserRepository
import mixit.support.MixitWebFilter
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.result.view.mustache.MustacheResourceTemplateLoader
import org.springframework.web.reactive.result.view.mustache.MustacheViewResolver

@SpringBootApplication
class Application {

    @Bean
    fun viewResolver() = MustacheViewResolver().apply {
        val prefix = "classpath:/templates/"
        val suffix = ".mustache"
        val loader = MustacheResourceTemplateLoader(prefix, suffix)
        setPrefix(prefix)
        setSuffix(suffix)
        setCompiler(Mustache.compiler().escapeHTML(false).withLoader(loader))
    }

    @Bean
    fun template(env: Environment) = ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(
                ConnectionString(env.getProperty("mongo.uri"))))

    @Bean
    fun mongoRepositoryFactory(db: ReactiveMongoOperations) = ReactiveMongoRepositoryFactory(db)

    @Bean
    fun dataInitializer(userRepository: UserRepository, eventRepository: EventRepository, sessionRepository: SessionRepository,
                        articleRepository: ArticleRepository) = ApplicationRunner {
        userRepository.initData()
        eventRepository.initData()
        sessionRepository.initData()
        articleRepository.initData()
        HandlerStrategies.withDefaults()
    }

    @Bean
    fun filter() = MixitWebFilter()

}

fun main(args: Array<String>) {
	SpringApplication.run(Application::class.java, *args)
}
