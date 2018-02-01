package mixit

import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Mustache.TemplateLoader
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication {

    @Bean
    fun mustacheCompiler(templateLoader: TemplateLoader): Mustache.Compiler =
            //Mustache.compiler().withEscaper(SimpleEscapers().HTML).withLoader(templateLoader)
            // TODO Find a way to disable HTML escaping before enabling user authentication
            Mustache.compiler().escapeHTML(false).withLoader(templateLoader)
}

fun main(args: Array<String>) {
    runApplication<MixitApplication>(*args)
}
