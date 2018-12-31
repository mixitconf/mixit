package mixit

import mixit.config.databaseConfig
import mixit.config.mailConfig
import mixit.config.webConfig
import org.springframework.fu.kofu.application


val app = application {
    configurationProperties<MixitProperties>(prefix = "mixit")
    enable(databaseConfig)
    enable(mailConfig)
    enable(webConfig)
}

fun main(args: Array<String>) {
    app.run()
}
