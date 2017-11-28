package mixit.util

import com.samskivert.mustache.Mustache
import mixit.MixitProperties
import mixit.model.User
import mixit.web.generateModelForExernalCall
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


/**
 * Test {@link EmailService}
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmailSenderTest {

    @Autowired
    lateinit var mustacheCompiler: Mustache.Compiler

    @Autowired
    lateinit var resourceLoader: ResourceLoader

    @Autowired
    lateinit var properties: MixitProperties

    @Autowired
    lateinit var messageSource: MessageSource

    @Autowired
    lateinit var cryptographer:Cryptographer

    val mailSenderMock: JavaMailSender = Mockito.mock(JavaMailSender::class.java)

    lateinit var emailSender: EmailSender

    @BeforeEach
    fun `init test`(){
        properties.aes.initvector = "RandomInitVector"
        properties.aes.key = "Bar12345Bar12345"
        // Service to test is not injected because we want to use  Spy to simulate the work of the mailSender
        emailSender = EmailSender(mustacheCompiler, resourceLoader, mailSenderMock, properties, messageSource, cryptographer)
    }

    @Test
    fun `open a mustache template to generate email in french`() {
        val user = User("test@gmail.com", "Guillaume", "EHRET", "dGVzdEBnbWFpbC5jb20=")
        user.token = "token-3455-dede"
        var params = generateModelForExernalCall("https://mixitconf.org", Locale.FRENCH, messageSource)
        params.put("user", user)

        val content = emailSender.openTemplate("email-token", params)

        Assertions.assertThat(content)
                // a message to say hello
                .contains("Bonjour Guillaume")
                // a link to our website
                .contains("<a href=\"https://mixitconf.org\">https://mixitconf.org</a>")
                // a link to be able to end the token
                .contains("<form action=\"https://mixitconf.org/signin\" method=\"post\">")
    }

    @Test
    fun `open a mustache template to generate email in english`() {
        var params = generateModelForExernalCall("https://mixitconf.org", Locale.ENGLISH, messageSource)
        params.put("user", createUser())

        val content = emailSender.openTemplate("email-token", params)

        Assertions.assertThat(content)
                // a message to say hello
                .contains("Hello Guillaume")
                // a link to our website
                .contains("<a href=\"https://mixitconf.org\">https://mixitconf.org</a>")
                // a link to be able to end the token
                .contains("<form action=\"https://mixitconf.org/signin\" method=\"post\">")
    }

    @Test
    fun `send an email to a user with specified subject`(){
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        BDDMockito.given(mailSenderMock.createMimeMessage()).willReturn(mimeMessage)

        emailSender.sendEmail("email-token", "subject", createUser(), Locale.FRENCH)

        // Message must be sent
        val argumentCaptor: ArgumentCaptor<MimeMessage> = ArgumentCaptor.forClass(MimeMessage::class.java)
        Mockito.verify(mailSenderMock, Mockito.atLeastOnce()).send(argumentCaptor.capture())

        Assertions.assertThat(argumentCaptor.value.subject).isEqualTo("subject")
        Assertions.assertThat(argumentCaptor.value.getRecipients(Message.RecipientType.TO))
                .isEqualTo(arrayOf(InternetAddress("test@gmail.com")))
    }

    @Test
    fun `send an email with a connexion token to a user in english`(){
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        BDDMockito.given(mailSenderMock.createMimeMessage()).willReturn(mimeMessage)

        emailSender.sendUserTokenEmail(createUser(), Locale.ENGLISH)

        // Message must be sent
        val argumentCaptor: ArgumentCaptor<MimeMessage> = ArgumentCaptor.forClass(MimeMessage::class.java)
        Mockito.verify(mailSenderMock, Mockito.atLeastOnce()).send(argumentCaptor.capture())

        Assertions.assertThat(argumentCaptor.value.subject).isEqualTo("Connection token to login on https://mixitconf.org")
        Assertions.assertThat(argumentCaptor.value.getRecipients(Message.RecipientType.TO))
                .isEqualTo(arrayOf(InternetAddress("test@gmail.com")))
        Assertions.assertThat(argumentCaptor.value.content.toString())
                // a message to say hello
                .contains("Hello Guillaume")
                // a link to our website
                .contains("<a href=\"http://localhost:8080\">http://localhost:8080</a>")
                // a link to be able to end the token
                .contains("<form action=\"http://localhost:8080/signin\" method=\"post\">")
    }

    private fun createUser(): User {
        val user = User("test@gmail.com", "Guillaume", "EHRET", "lnGW8QagnVzABAjptgMCJg==")
        user.token = "token-3455-dede"
        return user
    }
}