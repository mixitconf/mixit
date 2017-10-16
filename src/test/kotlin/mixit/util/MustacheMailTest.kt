package mixit.util

import com.samskivert.mustache.Mustache
import mixit.MixitProperties
import mixit.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ResourceLoader
import org.springframework.mail.MailSender
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.InputStreamReader
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender


/**
 * @author Dev-Mind <guillaume></guillaume>@dev-mind.fr>
 * @since 12/10/17.
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MustacheMailTest {

    @Autowired
    lateinit var mustacheCompiler: Mustache.Compiler

    @Autowired
    lateinit var mixitProperties: MixitProperties

    @Autowired
    lateinit var resourceLoader: ResourceLoader

    @Autowired
    lateinit var mailSender: JavaMailSender

    @Test
    fun `load a mail template`() {
        val templateName = "mail-token"
        val resource = resourceLoader.getResource("classpath:templates/${templateName}.mustache").inputStream
        val template = mustacheCompiler.compile(InputStreamReader(resource))
        val user = User("test@gmail.com", "firstname", "lastname", "test@gmail.com")
        user.token = "mytoken"

        assertThat(template.execute(mapOf(
                Pair("user", user),
                Pair("context", mixitProperties)))
        )
        .isNotEmpty().contains("mytoken")

        var msg = SimpleMailMessage()
        msg.to = arrayOf("gui.ehret@gmail.com")
        msg.from = "contact@mix-it.fr"
        msg.subject= "Jeton de connexion pour se connecter au site https://mixitconf.org"
        msg.text = template.execute(mapOf(
                Pair("user", user),
                Pair("context", mixitProperties),
                Pair("token", "mytoken")))
//
//        EmailBuilder(
//                "gui.ehret@gmail.com",
//                "Jeton de connexion pour se connecter au site https://mixitconf.org",
//                template.execute(mapOf(
//                        Pair("user", user),
//                        Pair("context", mixitProperties),
//                        Pair("token", "mytoken")))
//        ).send(mailSender)

        //mailSender.send(msg)
    }
}