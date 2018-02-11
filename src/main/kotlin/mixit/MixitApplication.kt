package mixit

import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Mustache.TemplateLoader
import mixit.web.MustacheEscapers
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication {

    @Bean
    fun mustacheCompiler(templateLoader: TemplateLoader): Mustache.Compiler =
            Mustache.compiler().withEscaper(MustacheEscapers().HTML).withLoader(templateLoader)
}

fun main(args: Array<String>) {
    runApplication<MixitApplication>(*args)
}
