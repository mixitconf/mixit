package mixit.config

import com.samskivert.mustache.Mustache
import mixit.util.validator.StringEscapers
import org.springframework.boot.autoconfigure.mustache.MustacheProperties
import org.springframework.boot.autoconfigure.mustache.MustacheResourceTemplateLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import java.io.Reader

@Configuration
class TemplateConfig {
    @Bean
    fun mustacheCompiler(templateLoader: MixitMustacheTemplateLoader): Mustache.Compiler =
        Mustache.compiler().withEscaper(StringEscapers().HTML).withLoader(templateLoader)
}

/**
 * As we want to isolate partials in a sub directory we use a custom Mustache template
 * loader to be able to load them
 */
@Component
class MixitMustacheTemplateLoader(
    private val mustache: MustacheProperties,
    private val resourceLoader: ResourceLoader
) : MustacheResourceTemplateLoader(mustache.prefix, mustache.suffix) {

    companion object {
        /**
         * Directory in classpath:/templates where partials are presents
         */
        const val PARTIAL_PATH = "partials/"
    }

    override fun getTemplate(name: String): Reader {
        val resource = resourceLoader.getResource(
            "${mustache.prefix}$PARTIAL_PATH$name${mustache.suffix}"
        )
        if (resource.exists()) {
            return InputStreamReader(resource.inputStream, mustache.charsetName)
        }
        return InputStreamReader(
            resourceLoader.getResource("${mustache.prefix}$name${mustache.suffix}").inputStream,
            mustache.charsetName
        )
    }
}
