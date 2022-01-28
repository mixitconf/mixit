package mixit.config

import com.samskivert.mustache.Mustache
import mixit.util.StringEscapers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemplateConfig {
    @Bean
    fun mustacheCompiler(templateLoader: Mustache.TemplateLoader): Mustache.Compiler =
        Mustache.compiler().withEscaper(StringEscapers().HTML).withLoader(templateLoader)
}
