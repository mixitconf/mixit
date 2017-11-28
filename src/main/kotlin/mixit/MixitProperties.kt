package mixit

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("mixit")
data class MixitProperties(
    var baseUri: String,
    var contact: String,
    var drive: Drive,
    var aes: Aes) {

    data class Drive(
        var fr: DriveDocuments,
        var en: DriveDocuments)


    data class DriveDocuments(
        var sponsorform: String,
        var sponsor: String,
        var speaker: String,
        var press: String)

    data class Aes(
        var initvector: String,
        var key: String)
}


