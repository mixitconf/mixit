package mixit.integration

import mixit.model.Event
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventIntegrationTests(@Autowired val client: WebTestClient) {

    @Test
    fun `Find MiXiT 2016 event`() {
        client.get().uri("/api/event/mixit16").accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("\$.year").isEqualTo("2016")
                .jsonPath("\$.current").isEqualTo("false")
    }

    @Test
    fun `Find all events`() {
        client.get().uri("/api/event/").accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBodyList<Event>()
                .hasSize(8)

    }
    
}
