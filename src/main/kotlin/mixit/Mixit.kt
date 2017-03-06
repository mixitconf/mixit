package mixit

import com.mongodb.ConnectionString
import com.samskivert.mustache.Mustache
import mixit.util.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.mustache.MustacheResourceTemplateLoader
import org.springframework.web.reactive.result.view.mustache.MustacheViewResolver


@SpringBootApplication
class Mixit {

    @Bean
    fun viewResolver(messageSource: MessageSource) = MustacheViewResolver().apply {
        val prefix = "classpath:/templates/"
        val suffix = ".mustache"
        val loader = MustacheResourceTemplateLoader(prefix, suffix)
        setPrefix(prefix)
        setSuffix(suffix)
        setCompiler(Mustache.compiler().escapeHTML(false).withLoader(loader))
        setModelCustomizer({ model, exchange ->  customizeModel(model, exchange, messageSource) })
    }

    @Bean
    fun routerFunction(routesProvider: List<RouterFunctionProvider>) =
        routesProvider.map { it.invoke() }.reduce(RouterFunction<ServerResponse>::and)

    @Bean
    fun databaseFactory(env: Environment) = SimpleReactiveMongoDatabaseFactory(ConnectionString(env.getProperty("mongo.uri")))

    @Bean
    fun template(databaseFactory: ReactiveMongoDatabaseFactory) = ReactiveMongoTemplate(databaseFactory)

    @Bean
    fun filter(env: Environment) = MixitWebFilter(env.getProperty("baseUri"))

    @Bean
    fun markdownConverter() = MarkdownConverter()

}

fun main(args: Array<String>) {
    run(Mixit::class, *args)
}
