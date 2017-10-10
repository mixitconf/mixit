package mixit.util

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.util.store.MemoryDataStoreFactory
import mixit.MixitProperties
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*
import javax.mail.Session
import javax.mail.internet.MimeMessage


/**
 * Unit tests for {@link MailSender}
 */
class MailSenderTest {

    var mailSender: MailSender? = null
    var mixitProperties = MixitProperties()

    @Before
    fun init() {
        mixitProperties.googleapi.clientId = "577472768734-s33h2f2o4aad5o590clfppujlmp0pf1h.apps.googleusercontent.com"
        mixitProperties.googleapi.clientSecret = "JyZYrBlJGkHKxE8GiUq6KbDk"
        mixitProperties.googleapi.application = "cesar-mixit"
        mixitProperties.contact = "contact@mix-it.fr"

        mailSender = MailSender(
                GoogleNetHttpTransport.newTrustedTransport(),
                MemoryDataStoreFactory(),
                mixitProperties)
    }

    @Test
    fun createEmail(){
        val message = mailSender?.createEmail(
                "contact@mix-it.fr",
                "This a suject",
                "Hello MiXiT\n How are you ?")

        Assert.assertEquals("Hello MiXiT\n How are you ?", message?.content.toString())
    }
}