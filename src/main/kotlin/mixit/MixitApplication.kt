package mixit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

data class SpeakerStart(val year: Int, val login: String)

@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication {
    companion object {
        const val CURRENT_EVENT = "2024"
        const val TIMEZONE = "Europe/Paris"
        const val MIXIT = "MiXiT"
        const val MIXIT_EMAIL = "contact@mixitconf.org"

        val speakerStarInHistory = listOf(
            SpeakerStart(2015, "tastapod"),
            SpeakerStart(2016, "joel.spolsky"),
            SpeakerStart(2021, "pamelafox"),
            SpeakerStart(2015, "MattiSG"),
            SpeakerStart(2019, "bodil"),
            SpeakerStart(2015, "mojavelinux"),
            SpeakerStart(2017, "andrey.breslav"),
            SpeakerStart(2015, "ppezziardi"),
            SpeakerStart(2017, "rising.linda"),
            SpeakerStart(2018, "jhoeller"),
            SpeakerStart(2018, "sharonsteed"),
            SpeakerStart(2018, "allan.rennebo"),
            SpeakerStart(2013, "agilex"),
            SpeakerStart(2018, "laura.carvajal"),
            SpeakerStart(2018, "augerment"),
            SpeakerStart(2016, "dgageot"),
            SpeakerStart(2018, "romainguy"),
            SpeakerStart(2018, "graphicsgeek1"),
            SpeakerStart(2019, "andre"),
            SpeakerStart(2019, "mary"),
            SpeakerStart(2019, "Woody.Zuill"),
            SpeakerStart(2019, "james.carlson"),
            SpeakerStart(2019, "egorcenski"),
            SpeakerStart(2012, "ojuncu"),
            SpeakerStart(2015, "hsablonniere"),
            SpeakerStart(2014, "nitot"),
            SpeakerStart(2021, "guillaume.pitron"),
            SpeakerStart(2021, "antonio.casilli"),
            SpeakerStart(2021, "isabelle.collet"),
            SpeakerStart(2021, "sinatou-saka"),
            SpeakerStart(2021, "gaelduval"),
            SpeakerStart(2021, "esther"),
            SpeakerStart(2015, "FlorencePorcel")
        )
        val speakerStarInCurrentEvent = listOf(
            SpeakerStart(2022, "tariqkrim"),
            SpeakerStart(2022, "ophelie.coelho"),
            SpeakerStart(2022, "laurentldlc"),
            SpeakerStart(2022, "amcordier"),
            SpeakerStart(2022, "bootis"),
            SpeakerStart(2022, "RatZillaS")
        )
    }
}

fun main(args: Array<String>) {
    runApplication<MixitApplication>(*args)
}
