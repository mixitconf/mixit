package mixit.integration

import mixit.model.Event
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.test.StepVerifier


class EventIntegrationTests : AbstractIntegrationTests() {

    @Test
    fun `Find MiXiT 2016 event`() {
        StepVerifier.create(client.get().uri("http://localhost:$port/api/event/mixit16")
                .accept(APPLICATION_JSON).exchange()
                .then { r -> r.bodyToMono<Event>() })
                .consumeNextWith {
                    assertEquals(2016, it.year)
                    assertFalse(it.current)
                }
                .verifyComplete()
    }

    @Test
    fun `Find all events`() {
        StepVerifier.create(client.get().uri("http://localhost:$port/api/event/")
                .accept(APPLICATION_JSON).exchange()
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
