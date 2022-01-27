package mixit

import java.math.BigDecimal
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mixit")
class MixitProperties {
    lateinit var baseUri: String
    lateinit var contact: String
    lateinit var vimeoTchatUri: String
    lateinit var vimeoFluxUri: String
    lateinit var mixetteValue: BigDecimal

    val drive = Drive()
    val aes = Aes()
    val googleapi = GoogleApi()
    val feature = Feature()
    
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

    class Feature {
        var donation: Boolean = false
        var lottery: Boolean = false
        var lotteryResult: Boolean = false
        var email: Boolean = false
        var mixette: Boolean = false
    }
}
