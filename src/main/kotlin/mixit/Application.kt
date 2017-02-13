package mixit

import com.mongodb.ConnectionString
import com.samskivert.mustache.Mustache
import mixit.repository.ArticleRepository
import mixit.repository.EventRepository
import mixit.repository.SessionRepository
import mixit.repository.UserRepository
import mixit.support.MarkdownConverter
import mixit.support.MixitWebFilter
import mixit.support.run
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.web.reactive.result.view.mustache.MustacheResourceTemplateLoader
import org.springframework.web.reactive.result.view.mustache.MustacheViewResolver

@SpringBootApplication(exclude = arrayOf(MongoAutoConfiguration::class, MongoDataAutoConfiguration::class))
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
    fun databaseFactory(env: Environment) = SimpleReactiveMongoDatabaseFactory(ConnectionString(env.getProperty("mongo.uri")))

    @Bean
    fun template(databaseFactory: ReactiveMongoDatabaseFactory) = ReactiveMongoTemplate(databaseFactory)

    @Bean
    fun filter() = MixitWebFilter()

    @Bean
    fun markdownConverter() = MarkdownConverter()

    @Bean
    fun dataInitializer(userRepository: UserRepository, eventRepository: EventRepository, sessionRepository: SessionRepository,
                        articleRepository: ArticleRepository) = ApplicationRunner {
        userRepository.initData()
        eventRepository.initData()
        sessionRepository.initData()
        articleRepository.initData()
    }

}

fun main(args: Array<String>) {
    run(Application::class, *args)
}
