package mixit.integration

import mixit.model.Session
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientOperations
import reactor.test.StepVerifier

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SessionIntegrationTests {

    @LocalServerPort
    lateinit var port: Integer

    val operations = WebClientOperations.builder(WebClient.builder(ReactorClientHttpConnector()).build()).build()

    @Test
    fun `Find Dan North session`() {
        StepVerifier.create(operations.get().uri("http://localhost:$port/api/session/2421")
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
        StepVerifier.create(operations.get().uri("http://localhost:$port/api/mixit12/session/")
                .accept(APPLICATION_JSON).exchange()
                .flatMap { it.bodyToFlux<Session>() })
                .expectNextCount(27)
                .verifyComplete()

    }
}
