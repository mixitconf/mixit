package mixit.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.test


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebsiteIntegrationTests(@LocalServerPort port: Int) {

    private val client = WebClient.create("http://localhost:$port")

    @Test
    fun home() {
        client.get().uri("/").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/en/").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()
    }

    @Test
    fun about() {
        client.get().uri("/about").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/en/about").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()
    }

    @Test
    fun blog() {
        client.get().uri("/blog").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/en/blog").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/blog/talks-equipe--produit--pedagogie-et-design-a-mixit-2017").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/en/blog/teams--products--pedagogy-and-design-talks-at-mixit-2017").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()
    }

    @Test
    fun sponsors() {
        client.get().uri("/sponsors").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/en/sponsors").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()
    }

    @Test
    fun talks() {
        client.get().uri("/2017").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/en/2017").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()


        client.get().uri("/2016").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/en/2016").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/talk/florent-pellet-clement-bouillier-emilien-pecoul-jean-helou-event-storming---comprendre-le-metier-autrement").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == PERMANENT_REDIRECT }
                .verifyComplete()

        client.get().uri("/2016/florent-pellet-clement-bouillier-emilien-pecoul-jean-helou-event-storming---comprendre-le-metier-autrement").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()

        client.get().uri("/en/talk/florent-pellet-clement-bouillier-emilien-pecoul-jean-helou-event-storming---comprendre-le-metier-autrement").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == PERMANENT_REDIRECT }
                .verifyComplete()

        client.get().uri("/en/2016/florent-pellet-clement-bouillier-emilien-pecoul-jean-helou-event-storming---comprendre-le-metier-autrement").accept(TEXT_HTML)
                .exchange()
                .test()
                .expectNextMatches { it.statusCode() == OK }
                .verifyComplete()
    }

}
