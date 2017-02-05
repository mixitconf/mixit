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
        val session = client
                .get()
                .uri("/api/session/2421")
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap { r -> r.bodyToFlux<Session>() }

        StepVerifier.create(session)
                .consumeNextWith {
                    assertEquals("Selling BDD to the Business", it.title)
                    assertEquals("North", it.speakers.iterator().next().lastname)
                }
                .verifyComplete()
    }

    @Test
    fun `Find MiXiT 2012 sessions`() {
        val sessions = client
                .get()
                .uri("/api/mixit12/session/")
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap { it.bodyToFlux<Session>() }

        StepVerifier.create(sessions)
                .expectNextCount(27)
                .verifyComplete()

    }
}
