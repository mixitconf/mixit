package mixit.config

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import mixit.MixitProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource

@Configuration
@Profile("cloud", "service-mail")
class GmailApiConfig {

    @Bean
    fun jacksonFactory() = JacksonFactory.getDefaultInstance()

    @Bean
    fun dataStoreFactory() = MemoryDataStoreFactory.getDefaultInstance()

    @Bean
    fun httpTransport() = GoogleNetHttpTransport.newTrustedTransport()

    @Bean
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
    fun gmailService(properties: MixitProperties) = Gmail
            .Builder(httpTransport(), jacksonFactory(), authorize(properties))
            .setApplicationName(properties.googleapi.appname).build()
}
