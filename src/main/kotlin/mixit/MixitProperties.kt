package mixit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mixit")
class MixitProperties {
    lateinit var baseUri: String
    lateinit var contact: String
    lateinit var vimeoTchatUri: String
    lateinit var vimeoFluxUri: String

    val drive = Drive()
    val aes = Aes()
    val googleapi = GoogleApi()

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

    class GoogleApi {
        lateinit var clientid: String
        lateinit var p12path: String
        lateinit var user: String
        lateinit var appname: String
    }
}
