package mixit.util

import com.samskivert.mustache.Mustache
import mixit.model.User
import mixit.web.generateModelForExernalCall
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*


/**
 * Test {@link TemplateService}
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TemplateServiceTest{
    @Autowired
    lateinit var mustacheCompiler: Mustache.Compiler

    @Autowired
    lateinit var resourceLoader: ResourceLoader

    @Autowired
    lateinit var messageSource: MessageSource

    lateinit var templateService: TemplateService

    @BeforeEach
    fun `init test`() {
        // Service to test is not injected because we want to use  Spy to simulate the work of the mailSender
        templateService = TemplateService(mustacheCompiler, resourceLoader)
    }

    @Test
    fun `open a mustache template to generate email in french`() {
        val user = User("test@gmail.com", "Guillaume", "EHRET", "dGVzdEBnbWFpbC5jb20=")
        user.token = "token-3455-dede"
        val params = generateModelForExernalCall("https://mixitconf.org", Locale.FRENCH, messageSource)
        params.put("user", user)
        params.put("encodedemail", "test@gmail.com")

        val content = templateService.load("email-token", params)

        Assertions.assertThat(content)
                // a message to say hello
                .contains("Bonjour Guillaume")
                // a link to our website
                .contains("<a href=\"https://mixitconf.org\">https://mixitconf.org</a>")
                // a link to be able to end the token
                .contains("<a href=\"https://mixitconf.org/signin/token-3455-dede/test&#64;gmail.com\" class=\"button mxt-button\">Log In</a>")
    }

    @Test
    fun `open a mustache template to generate email in english`() {
        val params = generateModelForExernalCall("https://mixitconf.org", Locale.ENGLISH, messageSource)
        params.put("user", createUser())
        params.put("encodedemail", "test@gmail.com")

        val content = templateService.load("email-token", params)

        Assertions.assertThat(content)
                // a message to say hello
                .contains("Hello Guillaume")
                // a link to our website
                .contains("<a href=\"https://mixitconf.org\">https://mixitconf.org</a>")
                // a link to be able to end the token
                .contains("<a href=\"https://mixitconf.org/signin/token-3455-dede/test&#64;gmail.com\" class=\"button mxt-button\">Log In</a>")
    }

    private fun createUser(): User {
        val user = User("test@gmail.com", "Guillaume", "EHRET", "lnGW8QagnVzABAjptgMCJg==")
        user.token = "token-3455-dede"
        return user
    }
}
