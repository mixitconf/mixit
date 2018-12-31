package mixit.config

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import mixit.MixitProperties
import mixit.util.*
import mixit.util.validator.EmailValidator
import org.springframework.core.io.ClassPathResource
import org.springframework.fu.kofu.configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

val mailConfig = configuration {
    beans {
        bean<EmailService>()
        if (profiles.contains("cloud")) {
            bean<EmailSender> { GmailApiSender(ref(), ref()) }
            beans {
                bean("gmailService") {
                    val properties = ref<MixitProperties>()
                    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
                    val jacksonFactory = JacksonFactory.getDefaultInstance()
                    val credentials = GoogleCredential.Builder()
                            .setTransport(httpTransport)
                            .setJsonFactory(jacksonFactory)
                            .setServiceAccountId(properties.googleapi.clientid)
                            .setServiceAccountPrivateKeyFromP12File(ClassPathResource(properties.googleapi.p12path).file)
                            .setServiceAccountScopes(listOf(GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_INSERT, GmailScopes.MAIL_GOOGLE_COM))
                            .setServiceAccountUser(properties.googleapi.user)
                            .build()
                            .apply { this.refreshToken }
                    Gmail
                            .Builder(httpTransport, jacksonFactory, credentials)
                            .setApplicationName(properties.googleapi.appname).build()
                }
            }
        } else {
            bean<EmailSender> { GmailSmtpSender(ref()) }
        }
        bean<JavaMailSender> { JavaMailSenderImpl() }
        bean<TemplateService>()
        bean<EmailValidator>()
    }
}