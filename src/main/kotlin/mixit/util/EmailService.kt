package mixit.util

import com.samskivert.mustache.Mustache
import mixit.MixitProperties
import mixit.model.User
import mixit.web.generateModelForExernalCall
import mixit.web.service.EmailSenderException
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.Locale

@Component
class TemplateService(
    private val mustacheCompiler: Mustache.Compiler,
    private val resourceLoader: ResourceLoader
) {

    fun load(templateName: String, context: Map<String, Any>): String {
        val resource = resourceLoader.getResource("classpath:templates/$templateName.mustache").inputStream
        val template = mustacheCompiler.compile(InputStreamReader(resource))
        return template.execute(context)
    }
}

@Component
class EmailService(
    private val properties: MixitProperties,
    private val messageSource: MessageSource,
    private val cryptographer: Cryptographer,
    private val emailSender: EmailSender,
    private val templateService: TemplateService
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun send(templateName: String, user: User, locale: Locale) {

        val subject = messageSource.getMessage("$templateName-subject", null, locale)
        val email = cryptographer.decrypt(user.email)!!

        runCatching {
            generateModelForExernalCall(properties.baseUri, locale, messageSource).apply {
                put("user", user)
                put("encodedemail", URLEncoder.encode(email.encodeToBase64(), "UTF-8"))

                val content = templateService.load(templateName, this)
 //               emailSender.send(EmailMessage(email, subject, content))
            }
        }.onFailure {
            logger.error("Not possible to send email [$subject] to ${user.email}", it)
            throw EmailSenderException("Error when system send the mail $subject")
        }
    }
}
