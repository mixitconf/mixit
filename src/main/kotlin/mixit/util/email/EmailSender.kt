package mixit.util.email

import com.google.api.services.gmail.Gmail
import com.google.api.services.gmail.model.Message
import mixit.MixitProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Email
 */
data class EmailMessage(val to: String, val subject: String, val content: String, val bcc: String? = null)

/**
 * An email sender is able to send an HTML message via email to a consignee
 */
interface EmailSender {
    fun send(email: EmailMessage)
}

/**
 * Gmail API service is used in cloud mode to send email
 */
@Component
@Profile("service-mail")
class GmailApiSender(private val properties: MixitProperties, private val gmailService: Gmail) : EmailSender {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun send(email: EmailMessage) {
        val session = Session.getDefaultInstance(Properties(), null)
        val message = MimeMessage(session)

        message.setFrom(InternetAddress(properties.contact))
        message.addRecipient(javax.mail.Message.RecipientType.TO, InternetAddress(email.to))
        if (email.bcc != null) {
            message.addRecipients(javax.mail.Message.RecipientType.BCC, email.bcc)
        }
        message.subject = email.subject
        message.setContent(email.content, "${MediaType.TEXT_HTML_VALUE}; charset=UTF-8")

        val buffer = ByteArrayOutputStream()
        message.writeTo(buffer)

        val emailMessage = Message()
        emailMessage.encodeRaw(buffer.toByteArray())

        gmailService.users().messages().send("me", emailMessage).execute().apply {
            logger.info("Mail Gmail API ${this.id} ${this.labelIds}")
        }
    }
}

/**
 * Gmail is used in developpement mode (via SMTP) to send email used for authentication
 * or for our different information messages
 */
@Component
@Profile("!service-mail")
class GmailSmtpSender(private val javaMailSender: JavaMailSender) : EmailSender {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun send(email: EmailMessage) {
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setTo(email.to)
        if (email.bcc != null) {
            helper.setBcc(email.bcc)
        }
        helper.setSubject(email.subject)
        message.setContent(email.content, MediaType.TEXT_HTML_VALUE)
        javaMailSender.send(message).apply {
            logger.debug("Mail SMTP")
        }
    }
}
