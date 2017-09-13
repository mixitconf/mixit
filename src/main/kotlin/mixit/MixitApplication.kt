package mixit

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Mustache.TemplateLoader
import mixit.util.run
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean


@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication {

    @Bean
    fun mustacheCompiler(templateLoader: TemplateLoader): Mustache.Compiler =
            // TODO Find a way to disable HTML escaping before enabling user authentication
            Mustache.compiler().escapeHTML(false).withLoader(templateLoader)

    /**
     * Bean used to call the Google Api Services
     */
    @Bean
    fun httpTransport() = GoogleNetHttpTransport.newTrustedTransport()

    /**
     * Google API credentials are stored in memory
     */
    @Bean
    fun memoryDataStoreFactory() = MemoryDataStoreFactory()
}

fun main(args: Array<String>) {
    run(MixitApplication::class, *args)
}
