package mixit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mixit")
class MixitProperties {
    lateinit var baseUri: String
    lateinit var contact: String
    val drive = Drive()
    val aes = Aes()
    val sendgrid = MailProvider()
    val elasticmail = MailProvider()

    class Drive {
        val fr = DriveDocuments()
        val en = DriveDocuments()

        class DriveDocuments {
            lateinit var sponsorform: String
            lateinit var sponsor: String
            lateinit var speaker: String
            lateinit var press: String
        }
    }

    class Aes {
        lateinit var initvector: String
        lateinit var key: String
    }

    class MailProvider {
        lateinit var apikey: String
        lateinit var host: String
        lateinit var version: String
    }
}


