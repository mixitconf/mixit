package mixit.integration

import mixit.Application
import mixit.model.User
import mixit.support.bodyToFlux
import org.junit.jupiter.api.*
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest.GET
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

class UserIntegrationTests {

    val baseUrl = "http://localhost:8081/api/user/"
    val application = Application(port = 8081)
    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    @BeforeEach
    fun before() = application.start()

    @Test
    fun findAll() {
        val response = webClient.exchange(GET(baseUrl).accept(APPLICATION_JSON_UTF8).build())
        StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(User::class)})
                .consumeNextWith { assert(it == User(1L, "Robert")) }
                .consumeNextWith { assert(it == User(2L, "Raide"))  }
                .consumeNextWith { assert(it == User(3L, "Ford")) }
                .expectComplete()
                .verify()
    }

    @AfterEach
    fun after() = application.stop()

}
