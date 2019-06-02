package mixit.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebsiteIntegrationTests(@Autowired val client: WebTestClient) {

    @Test
    fun home() {
        client.get().uri("/").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).contains("La conférence Lyonnaise avec des crêpes et du cœur")
                }

        client.get().uri("/en/").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).contains("The conference from Lyon with crêpes and love")
                }
    }

    @Test
    fun about() {
        client.get().uri("/about").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).contains("Cyril Lacote")
                }

        client.get().uri("/en/about").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).contains("Cyril Lacote")
                }
    }

    @Test
    fun blog() {
        client.get().uri("/blog").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).contains("Lire la suite ...")
                }

        client.get().uri("/en/blog").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).contains("Read more...")
                }

        client.get().uri("/blog/talks-equipe-produit-pedagogie-et-design-a-mixit-2017").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).contains("Voici un extrait du programme pour ceux qui veulent")
                }

        client.get().uri("/en/blog/teams-products-pedagogy-and-design-talks-at-mixit-2017").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<String>()
                .consumeWith {
                    assertThat(it.responseBody).contains("Let me give you a preview of the MiXiT 2017 program for those who want")
                }
    }

    @Test
    fun sponsors() {
        client.get().uri("/sponsors").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful

        client.get().uri("/en/sponsors").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
    }

    @Test
    fun talks() {
        client.get().uri("/2017").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful

        client.get().uri("/en/2017").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful


        client.get().uri("/2018").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful

        client.get().uri("/en/2018").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful

        client.get().uri("/talk/florent-pellet-clement-bouillier-emilien-pecoul-jean-helou-event-storming-comprendre-le-metier-autrement").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful


        client.get().uri("/2016/florent-pellet-clement-bouillier-emilien-pecoul-jean-helou-event-storming-comprendre-le-metier-autrement").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful


        client.get().uri("/en/talk/florent-pellet-clement-bouillier-emilien-pecoul-jean-helou-event-storming-comprendre-le-metier-autrement").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful

        client.get().uri("/en/2016/florent-pellet-clement-bouillier-emilien-pecoul-jean-helou-event-storming-comprendre-le-metier-autrement").accept(TEXT_HTML)
                .exchange()
                .expectStatus().is2xxSuccessful
    }

}
