package mixit.integration

import mixit.model.Talk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TalkIntegrationTests(@Autowired val client: WebTestClient) {

    @Test
    fun `Find Dan North talk`() {
        client.get().uri("/api/talk/2421").accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody()
                .jsonPath("\$.title").isEqualTo("Selling BDD to the Business")
                .jsonPath("\$.speakerIds").isEqualTo("tastapod")
    }

    @Test
    fun `Find MiXiT 2012 talks`() {
        client.get().uri("/api/2012/talk").accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBodyList<Talk>()
                .hasSize(27)
    }

}
