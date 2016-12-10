package fr.mixit.controller

import fr.mixit.Application
import fr.mixit.model.User
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.client.reactive.ClientRequest.GET
import org.springframework.web.client.reactive.WebClient
import reactor.test.StepVerifier

class UserIntegrationTests {

    val application = Application()
    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    @Before
    fun before() = application.start()


    @After
    fun after() = application.stop()

    @Test
    fun findAll() {
        val response = webClient.exchange(GET("http://localhost:8080/user/").accept(APPLICATION_JSON_UTF8).build())
        StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(User.Instance::class.java)})
                .consumeNextWith { assert(it == User.Instance(1L, "Robert")) }
                .consumeNextWith { assert(it == User.Instance(2L, "Raide"))  }
                .consumeNextWith { assert(it == User.Instance(3L, "Ford")) }
                .expectComplete()
                .verify()
    }
}
