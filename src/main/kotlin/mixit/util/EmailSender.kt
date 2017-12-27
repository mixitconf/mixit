package mixit.util

import com.sendgrid.*
import mixit.MixitProperties
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.io.IOException

/**
 * Email
 */
data class EmailMessage(val to: String, val subject: String, val content: String)

/**
 * An email sender is able to send an HTML message via email to a consignee
 */
interface EmailSender {
    fun send(email: EmailMessage)
}

/**
 * We can have an email sender to manage our authentication phase ...
 */
interface AuthentEmailSender : EmailSender

/**
 * ... or for send information messages to our users
 */
interface MessageEmailSender : EmailSender

/**
 * Gmail is used in developpement mode (via SMTP) to send email used for authentication
 * or for our different information messages
 */
@Component
@Profile("!cloud")
class GmailSender(private val javaMailSender: JavaMailSender) : AuthentEmailSender, MessageEmailSender {

    override fun send(email: EmailMessage) {
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setTo(email.to)
        helper.setSubject(email.subject)
        message.setContent(email.content, MediaType.TEXT_HTML_VALUE)
        javaMailSender.send(message)
    }
}

/**
 * Elastic email is the sender used on our cloud instances for our authentication emails
 */
@Component
@Profile("cloud")
class ElasticEmailSender(private val properties: MixitProperties) : AuthentEmailSender {

    override fun send(email: EmailMessage) {

        val result = WebClient.create(properties.elasticmail.host!!)
                .post()
                .uri("/${properties.elasticmail.version}/email/send")
                .body(BodyInserters
                        .fromFormData("apikey", properties.elasticmail.apikey!!)
                        .with("from", properties.contact)
                        .with("fromName", "MiXiT")
                        .with("to", email.to)
                        .with("subject", email.subject)
                        .with("isTransactional", "true")
                        .with("body", email.content))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ElasticEmailResponse::class.java)
                .block()

        if (result?.success == false) {
            throw RuntimeException(result.error)
        }
    }
}

/**
 * Response returned by elastic email
 */
data class ElasticEmailResponse(val success: Boolean, val error: String? = null, val data: Any? = null)

/**
 * Send grid is the sender used on our cloud instances to send information messages
 */
@Component
@Profile("cloud")
class SendGridSender(private val properties: MixitProperties) : MessageEmailSender{

    override fun send(email: EmailMessage) {

        val mail = Mail(
                Email(properties.contact, "MiXiT"),
                email.subject,
                Email(email.to),
                Content(MediaType.TEXT_HTML_VALUE, email.content))

        val sendGrid = SendGrid(properties.sendgrid.apikey)

        try {
            val request = Request()
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            sendGrid.api(request)
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }
}

