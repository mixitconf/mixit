package mixit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mixit")
class MixitProperties {
    var baseUri: String? = null
    val admin = Credential()
    val drive = Drive()

    class Credential {
        var username: String? = null
        var password: String? = null
    }

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
}


