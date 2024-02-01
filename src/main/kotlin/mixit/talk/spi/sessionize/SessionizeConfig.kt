package mixit.talk.spi.sessionize

import mixit.MixitProperties
import mixit.talk.model.CachedTalk
import mixit.talk.spi.CfpSynchronizer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Qualifier
annotation class Sessionize

@Configuration
class SessionizeConfig {
    @Bean
    @Sessionize
    fun sessionizeWebClient(properties: MixitProperties): WebClient =
        WebClient.builder()
            .baseUrl("https://sessionize.com/api/v2/${properties.sessionizeKey}/view/All")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
}
