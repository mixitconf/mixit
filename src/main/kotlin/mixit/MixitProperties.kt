package mixit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mixit")
class MixitProperties {
    var baseUri: String? = null
    var contact: String? = null
    val admin = Credential()
    val drive = Drive()
    val googleapi = GoogleApi()

    class Credential {
        var email: String? = null
        var token: String? = null
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

    class GoogleApi {
        var clientId: String? = null
        var clientSecret: String? = null
        var redirectUri: String? = null
        var authUri: String? = null
        var token_uri: String? = null
        var application: String? = null
    }
}


