package mixit.integration

import mixit.model.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import reactor.test.StepVerifier
import org.springframework.web.reactive.function.client.bodyToFlux

class SessionIntegrationTests : AbstractIntegrationTests() {


    @Test
    fun `Find Dan North session`() {
        StepVerifier.create(client.get().uri("http://localhost:$port/api/session/2421")
                .accept(APPLICATION_JSON).exchange()
                .flatMap { r -> r.bodyToFlux<Session>() })
                .consumeNextWith {
                    assertEquals("Selling BDD to the Business", it.title)
                    assertEquals("NORTH", it.speakers.iterator().next().lastname)
                }
                .verifyComplete()
    }

    @Test
    fun `Find MiXiT 2012 sessions`() {
        StepVerifier.create(client.get().uri("http://localhost:$port/api/mixit12/session/")
                .accept(APPLICATION_JSON).exchange()
                .flatMap { it.bodyToFlux<Session>() })
                .expectNextCount(27)
                .verifyComplete()

    }
}
