package mixit.util

import com.samskivert.mustache.Mustache
import mixit.MixitProperties
import mixit.model.User
import mixit.web.generateModel
import org.commonmark.internal.util.Escaping
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.InputStreamReader
import java.util.*
import javax.mail.MessagingException

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 15/10/17.
 */
@Service
class EmailService(private val mustacheCompiler: Mustache.Compiler,
                   private val resourceLoader: ResourceLoader,
                   private val mailSender: JavaMailSender,
                   private val properties: MixitProperties,
                   private val messageSource: MessageSource) {

    private val logger = LoggerFactory.getLogger(this.javaClass)


    fun sendUserTokenEmail(user: User, locale: Locale) {
        logger.info("A token was sent to ${user.email}")
        sendEmail("email-token",
                messageSource.getMessage("email-token-subject", null, locale),
                user,
                locale)
    }

    fun sendEmail(templateName: String, subject: String, user: User, locale: Locale) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            val context = generateModel(properties.baseUri!!, locale, messageSource)
            context.put("user", user)
            context.put("encodedemail", Escaping.escapeHtml(user.email!!, true))

            message.setContent(openTemplate(templateName, context), "text/html")
            helper.setTo(user.email!!)
            helper.setSubject(subject)

            mailSender.send(message)

        } catch (e: MessagingException) {
            logger.error(String.format("Not possible to send email [%s] to %s", subject, user.email), e)
            throw RuntimeException("Error when system send the mail " + subject, e)
        }
    }

    fun openTemplate(templateName: String, context: Map<String, Any>): String {
        val resource = resourceLoader.getResource("classpath:templates/${templateName}.mustache").inputStream
        val template = mustacheCompiler.compile(InputStreamReader(resource))

        return template.execute(context)
    }

}