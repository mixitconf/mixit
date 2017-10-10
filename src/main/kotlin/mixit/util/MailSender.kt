package mixit.util

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.MemoryDataStoreFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.GmailScopes
import com.google.api.services.gmail.model.Message
import mixit.MixitProperties
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.*
import com.sun.xml.internal.messaging.saaj.packaging.mime.MessagingException
import java.io.ByteArrayOutputStream
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


@Component
class MailSender(val httpTransport: HttpTransport,
                 val memoryDataStoreFactory: MemoryDataStoreFactory,
                 val mixitProperties: MixitProperties) {

    /**
     * Send an email from the user's mailbox to its recipient.
     * @param userId User's email address. The special value "me" can be used to indicate the authenticated user.
     */
    @Throws(MessagingException::class, IOException::class)
    fun sendMessage(userId: String, email: MimeMessage): Message {
        var message = createMessageWithEmail(email)
        return getGmailService().users().messages().send(userId, message).execute()
    }

    /**
     * Create a MimeMessage using the parameters provided.
     */
    @Throws(MessagingException::class)
    fun createEmail(to: String, subject: String, bodyText: String): MimeMessage {
        val props = Properties()
        val session = Session.getDefaultInstance(props, null)

        val email = MimeMessage(session)

        email.setFrom(InternetAddress(mixitProperties.contact))
        email.addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(to))
        email.setSubject(subject)
        email.setText(bodyText)
        return email
    }

    /**
     * Creates an authorized Credential object.
     */
    @Throws(IOException::class)
    private fun authorize(): Credential {
        val flow = GoogleAuthorizationCodeFlow.Builder(httpTransport,
                                                       JacksonFactory.getDefaultInstance(),
                                                       mixitProperties.googleapi.clientId,
                                                       mixitProperties.googleapi.clientSecret,
                                                       Arrays.asList(GmailScopes.GMAIL_LABELS))
                .setDataStoreFactory(memoryDataStoreFactory)
                .setAccessType("offline")
                .build()

        return AuthorizationCodeInstalledApp(flow, LocalServerReceiver()).authorize("user")
    }

    /**
     * Build and return an authorized Gmail client service.
     */
    @Throws(IOException::class)
    private fun getGmailService(): Gmail {
        val credential = authorize()
        return Gmail.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName(mixitProperties.googleapi.application)
                .build()
    }

    /**
     * Create a Message from an email
     */
    @Throws(MessagingException::class, IOException::class)
    private fun createMessageWithEmail(email: MimeMessage): Message {
        val baos = ByteArrayOutputStream()
        email.writeTo(baos)
        val encodedEmail = Base64.getEncoder().encodeToString(baos.toByteArray())
        val message = Message()
        message.setRaw(encodedEmail)
        return message
    }
}