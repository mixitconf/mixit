package mixit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mixit")
class MixitProperties {
    var baseUri: String? = null
    val admin = Credential()
    val drive = Drive()
    val oauth = Oauth()

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

    class Oauth {
        val github = Credential()
        val google = Credential()
        val twitter = Credential()

        class Credential {
            var apiKey: String? = null
            var clientSecret: String? = null
        }
    }

}


