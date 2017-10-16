package mixit.util

import com.samskivert.mustache.Mustache
import mixit.MixitProperties
import mixit.model.User
import mixit.web.generateModel
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito
import org.mockito.Mock
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import javax.mail.Session
import javax.mail.internet.MimeMessage
import org.mockito.ArgumentCaptor
import javax.mail.Address
import javax.mail.Message
import javax.mail.internet.InternetAddress


/**
 * Test {@link EmailService}
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmailServiceTest{

    @Autowired
    lateinit var mustacheCompiler: Mustache.Compiler

    @Autowired
    lateinit var resourceLoader: ResourceLoader

    @Autowired
    lateinit var properties: MixitProperties

    @Autowired
    lateinit var messageSource: MessageSource

    val mailSenderMock: JavaMailSender = Mockito.mock(JavaMailSender::class.java)

    lateinit var emailService: EmailService

    @BeforeEach
    fun `init test`(){
        // Service to test is not injected because we want to use  Spy to simulate the work of the mailSender
        emailService = EmailService(mustacheCompiler, resourceLoader, mailSenderMock, properties, messageSource)
    }

    @Test
    fun `open a mustache template to generate email in french`() {
        val user = User("test@gmail.com", "Guillaume", "EHRET", "test@gmail.com")
        user.token = "token-3455-dede"
        var params = generateModel("https://mixitconf.org", Locale.FRENCH, messageSource)
        params.put("user", user)

        val content = emailService.openTemplate("email-token", params)

        Assertions.assertThat(content)
                // a message to say hello
                .contains("Bonjour Guillaume")
                // a link to our website
                .contains("<a href=\"https://mixitconf.org\">https://mixitconf.org</a>")
                // a link to be able to end the token
                .contains("<a href=\"https://mixitconf.org/test@gmail.com/token-3455-dede\">token-3455-dede</a>")
    }

    @Test
    fun `open a mustache template to generate email in english`() {
        var params = generateModel("https://mixitconf.org", Locale.ENGLISH, messageSource)
        params.put("user", createUser())

        val content = emailService.openTemplate("email-token", params)

        Assertions.assertThat(content)
                // a message to say hello
                .contains("Hello Guillaume")
                // a link to our website
                .contains("<a href=\"https://mixitconf.org\">https://mixitconf.org</a>")
                // a link to be able to end the token
                .contains("<a href=\"https://mixitconf.org/test@gmail.com/token-3455-dede\">token-3455-dede</a>")
    }

    @Test
    fun `send an email to a user with specified subject`(){
        val mimeMessage = MimeMessage(Session.getInstance(Properties()))
        BDDMockito.given(mailSenderMock.createMimeMessage()).willReturn(mimeMessage)

        emailService.sendEmail("email-token", "subject", createUser(), Locale.FRENCH)

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

        emailService.sendUserTokenEmail(createUser(), Locale.ENGLISH)

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
                .contains("<a href=\"http://localhost:8080/test@gmail.com/token-3455-dede\">token-3455-dede</a>")
    }

    private fun createUser(): User {
        val user = User("test@gmail.com", "Guillaume", "EHRET", "test@gmail.com")
        user.token = "token-3455-dede"
        return user
    }
}