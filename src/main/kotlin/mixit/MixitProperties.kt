package mixit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mixit")
class MixitProperties {
    var baseUri: String? = null
    var contact: String? = null
    val drive = Drive()
    val aes = Aes()
    val sendgrid = MailProvider()
    val elasticmail = MailProvider()

    class Drive {
        val fr = DriveDocuments()
        val en = DriveDocuments()

        class DriveDocuments {
            var sponsorform: String? = null
            var sponsor: String? = null
            var speaker: String? = null
            var press: String? = null
        }
    }

    class Aes {
        var initvector: String? = null
        var key: String? = null
    }

    class MailProvider {
        var apikey: String? = null
        var host: String? = null
        var version: String? = null
    }
}


