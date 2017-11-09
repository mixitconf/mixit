package mixit.integration

import mixit.model.Talk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.test.test


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TalkIntegrationTests(@LocalServerPort port: Int) {

    private val client = WebClient.create("http://localhost:$port")

    @Test
    fun `Find Dan North talk`() {
        client.get().uri("/api/talk/2421").accept(APPLICATION_JSON)
                .retrieve()
                .bodyToFlux<Talk>()
                .test()
                .consumeNextWith {
                    assertEquals("Selling BDD to the Business", it.title)
                    assertEquals("tastapod", it.speakerIds.iterator().next())
                }
                .verifyComplete()
    }

    @Test
    fun `Find MiXiT 2012 talks`() {
        client.get().uri("/api/2012/talk").accept(APPLICATION_JSON)
                .retrieve()
                .bodyToFlux<Talk>()
                .test()
                .expectNextCount(27)
                .verifyComplete()
    }

}
