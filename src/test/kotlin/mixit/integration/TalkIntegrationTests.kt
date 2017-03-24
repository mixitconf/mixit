package mixit.integration

import mixit.model.Talk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.test

class TalkIntegrationTests : AbstractIntegrationTests() {

    @Test
    fun `Find Dan North talk`() {
        client.get().uri("/api/talk/2421").accept(APPLICATION_JSON)
                .exchange()
                .flatMap { it.bodyToFlux<Talk>() }
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
                .exchange()
                .flatMap { it.bodyToFlux<Talk>() }
                .test()
                .expectNextCount(27)
                .verifyComplete()
    }

}
