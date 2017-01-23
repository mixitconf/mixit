package mixit.integration

import mixit.model.Event
import mixit.support.getJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class EventIntegrationTests {

    @LocalServerPort
    lateinit var port: Integer

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    @Test
    fun `Find MiXiT 2016 event`() {
        StepVerifier.create(webClient
                .getJson("http://localhost:$port/api/event/mixit16")
                .then { r -> r.bodyToMono<Event>() })
                .consumeNextWith {
                    assertEquals(2016, it.year)
                    assertFalse(it.current)
                }
                .verifyComplete()
    }

    @Test
    fun `Find all events`() {
        StepVerifier.create(webClient
                .getJson("http://localhost:$port/api/event/")
                .flatMap { it.bodyToFlux<Event>() })
                .consumeNextWith { assertEquals(2012, it.year) }
                .consumeNextWith { assertEquals(2013, it.year) }
                .consumeNextWith { assertEquals(2014, it.year) }
                .consumeNextWith { assertEquals(2015, it.year) }
                .consumeNextWith { assertEquals(2016, it.year) }
                .consumeNextWith { assertEquals(2017, it.year) }
                .verifyComplete()
    }
}
