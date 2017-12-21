package mixit.util

import com.samskivert.mustache.Mustache
import com.sendgrid.*
import mixit.MixitProperties
import mixit.model.User
import mixit.web.generateModelForExernalCall
import org.commonmark.internal.util.Escaping
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.http.MediaType
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import javax.mail.MessagingException


abstract class EmailSender(private val mustacheCompiler: Mustache.Compiler,
                           private val resourceLoader: ResourceLoader,
                           private val properties: MixitProperties,
                           private val messageSource: MessageSource,
                           private val cryptographer: Cryptographer) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun sendUserTokenEmail(user: User, locale: Locale) {
        sendEmail("email-token",
                messageSource.getMessage("email-token-subject", null, locale),
                user,
                locale)
    }

    fun sendEmail(templateName: String, subject: String, user: User, locale: Locale) {
        try {
            val context = generateModelForExernalCall(properties.baseUri!!, locale, messageSource)
            val email = cryptographer.decrypt(user.email!!)

            context.put("user", user)
            context.put("encodedemail", Escaping.escapeHtml(email, true))

            sendMessage(email!!, subject, openTemplate(templateName, context))

        } catch (e: MessagingException) {
            logger.error(String.format("Not possible to send email [%s] to %s", subject, user.email), e)
            throw RuntimeException("Error when system send the mail " + subject, e)
        }
    }

    fun openTemplate(templateName: String, context: Map<String, Any>): String {
        val resource = resourceLoader.getResource("classpath:templates/$templateName.mustache").inputStream
        val template = mustacheCompiler.compile(InputStreamReader(resource))

        return template.execute(context)
    }

    abstract fun sendMessage(email: String, subject: String, content: String);

}

/**
 * Mail sender used in developpement mode. We call Gmail via SMTP
 */
@Component
class GmailSender(mustacheCompiler: Mustache.Compiler,
                  resourceLoader: ResourceLoader,
                  properties: MixitProperties,
                  messageSource: MessageSource,
                  cryptographer: Cryptographer,
                  private val javaMailSender: JavaMailSender) : EmailSender(mustacheCompiler, resourceLoader, properties, messageSource, cryptographer) {

    override fun sendMessage(email: String, subject: String, content: String) {
        val message = javaMailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        message.setContent(content, "text/html")
        helper.setTo(email)
        helper.setSubject(subject)
        javaMailSender.send(message)
    }

}

/**
 * Mail sender used in staging or prod on CloudFoundry
 */
@Component
class ElasticEmailSender(mustacheCompiler: Mustache.Compiler,
                  resourceLoader: ResourceLoader,
                  private val properties: MixitProperties,
                  messageSource: MessageSource,
                  cryptographer: Cryptographer) : EmailSender(mustacheCompiler, resourceLoader, properties, messageSource, cryptographer) {

    override fun sendMessage(email: String, subject: String, content: String) {

        val result = WebClient.create(properties.elasticmail.host!!)
                .post()
                .uri("/${properties.elasticmail.version}/email/send")
                .body(BodyInserters
                        .fromFormData("apikey", properties.elasticmail.apikey!!)
                        .with("from", properties.contact)
                        .with("fromName", "MiXiT")
                        .with("to", email)
                        .with("subject", subject)
                        .with("isTransactional", "true")
                        .with("body", content))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ElasticEmailResponse::class.java)
                .block()

        if(result?.success == false){
            throw RuntimeException(result.error)
        }
    }

}
data class ElasticEmailResponse(val success: Boolean, val error: String? = null, val data: Object? = null)

/**
 * Mail sender used in staging or prod on CloudFoundry. We can use sendGrid
 */
@Component
class SendGridSender(mustacheCompiler: Mustache.Compiler,
                     resourceLoader: ResourceLoader,
                     private val properties: MixitProperties,
                     messageSource: MessageSource,
                     cryptographer: Cryptographer) : EmailSender(mustacheCompiler, resourceLoader, properties, messageSource, cryptographer) {

    override fun sendMessage(email: String, subject: String, content: String) {

        val mail = Mail(Email(properties.contact, "MiXiT"), subject, Email(email), Content("text/html", content))

        val sendGrid = SendGrid(properties.sendgrid.apikey)
        val request = Request()
        try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            val response = sendGrid.api(request)
            System.out.println("Response send grid status = ${response.getStatusCode()}")
            System.out.println("Response send grid body = ${response.getBody()}")
            System.out.println("Response send grid header = ${response.getHeaders()}")
        } catch (ex: IOException ) {
            throw ex;
        }
    }
}