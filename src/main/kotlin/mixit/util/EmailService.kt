package mixit.util

import com.samskivert.mustache.Mustache
import mixit.MixitProperties
import mixit.model.User
import mixit.web.generateModelForExernalCall
import org.commonmark.internal.util.Escaping
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import java.util.*
import javax.mail.MessagingException

@Component
class TemplateService(private val mustacheCompiler: Mustache.Compiler,
                      private val resourceLoader: ResourceLoader) {

    fun load(templateName: String, context: Map<String, Any>): String {
        val resource = resourceLoader.getResource("classpath:templates/$templateName.mustache").inputStream
        val template = mustacheCompiler.compile(InputStreamReader(resource))
        return template.execute(context)
    }
}

enum class EmailServiceUsage {
    AUTHENTICATION, INFORMATION
}

@Component
class EmailService(private val properties: MixitProperties,
                   private val messageSource: MessageSource,
                   private val cryptographer: Cryptographer,
                   private val authentEmailSender: AuthentEmailSender,
                   private val messageEmailSender: MessageEmailSender,
                   private val templateService: TemplateService) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun send(templateName: String, user: User, locale: Locale, usage: EmailServiceUsage) {

        val subject = messageSource.getMessage("${templateName}-subject", null, locale)
        val context = generateModelForExernalCall(properties.baseUri!!, locale, messageSource)
        val email = cryptographer.decrypt(user.email!!)!!

        try {
            context.put("user", user)
            context.put("encodedemail", Escaping.escapeHtml(email, true))
            val content = templateService.load(templateName, context)

            if (usage == EmailServiceUsage.AUTHENTICATION) {
                authentEmailSender.send(EmailMessage(email, subject, content))
            } else {
                messageEmailSender.send(EmailMessage(email, subject, content))
            }

        } catch (e: MessagingException) {
            logger.error(String.format("Not possible to send email [%s] to %s", subject, user.email), e)
            throw RuntimeException("Error when system send the mail " + subject, e)
        }
    }

}
