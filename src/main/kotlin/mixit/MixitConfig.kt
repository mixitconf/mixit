package mixit

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(DocDriveProperties::class)
class MixitConfig {
}

@ConfigurationProperties("drive")
class DocDriveProperties {
    val fr: DriveDocuments = DriveDocuments()
    val en: DriveDocuments = DriveDocuments()

    class DriveDocuments {
        var sponsorform: String? = null
        var sponsor: String? = null
        var speaker: String? = null
        var press: String? = null
    }
}
