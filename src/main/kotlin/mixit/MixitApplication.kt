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
class MixitApplication

fun main(args: Array<String>) {
    runApplication<MixitApplication>(*args)
}
