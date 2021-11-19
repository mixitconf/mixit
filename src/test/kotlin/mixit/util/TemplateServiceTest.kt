package mixit.util

import com.samskivert.mustache.Mustache
import java.util.Locale
import mixit.model.User
import mixit.web.generateModelForExernalCall
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.MessageSource
import org.springframework.core.io.ResourceLoader
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Test {@link TemplateService}
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TemplateServiceTest(
    @Autowired val mustacheCompiler: Mustache.Compiler,
    @Autowired val resourceLoader: ResourceLoader,
    @Autowired val messageSource: MessageSource
) {

    val templateService: TemplateService = TemplateService(mustacheCompiler, resourceLoader)

    @Test
    fun `Open a mustache template to generate email in french`() {
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
    fun `Open a mustache template to generate email in english`() {
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
