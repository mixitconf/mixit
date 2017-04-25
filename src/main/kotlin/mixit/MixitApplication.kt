package mixit

import com.samskivert.mustache.Mustache
import mixit.util.MarkdownConverter
import mixit.web.MixitWebFilter
import mixit.util.run
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mustache.MustacheResourceTemplateLoader
import org.springframework.boot.autoconfigure.mustache.reactive.MustacheViewResolver
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication {

    private val prefix = "classpath:/templates/"
    private val suffix = ".mustache"
    private val mustacheCompiler = Mustache
            .compiler()
            .escapeHTML(false)
            .withLoader(MustacheResourceTemplateLoader(prefix, suffix))

    @Bean
    fun viewResolver() = MustacheViewResolver(mustacheCompiler).apply {
        setPrefix(prefix)
        setSuffix(suffix)
    }

    @Bean
    fun filter(properties: MixitProperties) = MixitWebFilter(properties)

    @Bean
    fun markdownConverter() = MarkdownConverter()

}

fun main(args: Array<String>) {
    run(MixitApplication::class, *args)
}
