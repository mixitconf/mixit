package mixit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(MixitProperties::class)
class MixitApplication

fun main(args: Array<String>) {
    runApplication<MixitApplication>(*args)
}
