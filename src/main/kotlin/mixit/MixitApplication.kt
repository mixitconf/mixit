package mixit

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Mustache.TemplateLoader
import mixit.web.StringEscapers
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource


@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication {

    @Bean
    fun mustacheCompiler(templateLoader: TemplateLoader): Mustache.Compiler =
            Mustache.compiler().withEscaper(StringEscapers().HTML).withLoader(templateLoader)

    @Bean
    @Profile("cloud")
    fun jacksonFactory() = JacksonFactory.getDefaultInstance()

    @Bean
    @Profile("cloud")
    fun dataStoreFactory() = MemoryDataStoreFactory.getDefaultInstance()

    @Bean
    @Profile("cloud")
    fun httpTransport() = GoogleNetHttpTransport.newTrustedTransport()

    @Bean
    @Profile("cloud")
    fun authorize(properties: MixitProperties): Credential = GoogleCredential.Builder()
                .setTransport(httpTransport())
                .setJsonFactory(jacksonFactory())
                .setServiceAccountId(properties.googleapi.clientid)
                .setServiceAccountPrivateKeyFromP12File(ClassPathResource(properties.googleapi.p12path).file)
                .setServiceAccountScopes(listOf(GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_INSERT, GmailScopes.MAIL_GOOGLE_COM))
                .setServiceAccountUser(properties.googleapi.user)
                .build()
                .apply { this.refreshToken }

    @Bean
    @Profile("cloud")
    fun gmailService(properties: MixitProperties) = Gmail
            .Builder(httpTransport(), jacksonFactory(), authorize(properties))
            .setApplicationName(properties.googleapi.appname).build()

}

fun main(args: Array<String>) {
    runApplication<MixitApplication>(*args)
}
