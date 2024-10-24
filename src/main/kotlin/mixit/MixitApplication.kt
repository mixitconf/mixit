package mixit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication {
    companion object {
        const val CURRENT_EVENT = "2025"
        const val NEXT_EVENT = "2025"
        const val TIMEZONE = "Europe/Paris"
        const val MIXIT = "MiXiT"
        const val MIXIT_EMAIL = "contact@mixitconf.org"
    }
}

fun main(args: Array<String>) {
    runApplication<MixitApplication>(*args)
}
