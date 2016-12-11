package mixit.integration

import mixit.Application
import mixit.model.User
import org.junit.jupiter.api.*
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.client.reactive.ClientRequest.GET
import org.springframework.web.client.reactive.WebClient
import reactor.test.StepVerifier

class UserIntegrationTests {

    val baseUrl = "http://localhost:8080/api/user/"
    val application = Application()
    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    @BeforeEach
    fun before() = application.start()

    @Test
    fun findAll() {
        val response = webClient.exchange(GET(baseUrl).accept(APPLICATION_JSON_UTF8).build())
        StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(User.Instance::class.java)})
                .consumeNextWith { assert(it == User.Instance(1L, "Robert")) }
                .consumeNextWith { assert(it == User.Instance(2L, "Raide"))  }
                .consumeNextWith { assert(it == User.Instance(3L, "Ford")) }
                .expectComplete()
                .verify()
    }

    @AfterEach
    fun after() = application.stop()

}
