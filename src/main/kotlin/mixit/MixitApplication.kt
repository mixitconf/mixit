package mixit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication {
    companion object {
        val speakerStarInHistory = listOf(
            "tastapod",
            "joel.spolsky",
            "pamelafox",
            "MattiSG",
            "bodil",
            "mojavelinux",
            "andrey.breslav",
            "ppezziardi",
            "rising.linda",
            "jhoeller",
            "sharonsteed",
            "allan.rennebo",
            "agilex",
            "laura.carvajal",
            "augerment",
            "dgageot",
            "romainguy",
            "graphicsgeek1",
            "andre",
            "mary",
            "Woody.Zuill",
            "james.carlson",
            "egorcenski",
            "ojuncu",
            "hsablonniere",
            "nitot"
        )
        val speakerStarInCurrentEvent = listOf<String>()
    }
}

fun main(args: Array<String>) {
    runApplication<MixitApplication>(*args)
}
