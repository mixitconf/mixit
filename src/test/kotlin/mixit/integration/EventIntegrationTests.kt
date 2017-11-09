package mixit.integration

import mixit.model.Event
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.test.test


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventIntegrationTests(@LocalServerPort port: Int) {

    private val client = WebClient.create("http://localhost:$port")

    @Test
    fun `Find MiXiT 2016 event`() {
        client.get().uri("/api/event/mixit16").accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono<Event>()
                .test()
                .consumeNextWith {
                    assertEquals(2016, it.year)
                    assertFalse(it.current)
                }
                .verifyComplete()
    }

    @Test
    fun `Find all events`() {
        client.get().uri("/api/event/").accept(APPLICATION_JSON)
                .retrieve()
                .bodyToFlux<Event>()
                .test()
                .consumeNextWith { assertEquals(2012, it.year) }
                .consumeNextWith { assertEquals(2013, it.year) }
                .consumeNextWith { assertEquals(2014, it.year) }
                .consumeNextWith { assertEquals(2015, it.year) }
                .consumeNextWith { assertEquals(2016, it.year) }
                .consumeNextWith { assertEquals(2017, it.year) }
                .consumeNextWith { assertEquals(2018, it.year) }
                .verifyComplete()
    }
    
}
