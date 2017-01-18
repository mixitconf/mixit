package mixit.integration

import mixit.Application
import mixit.model.Session
import mixit.support.getJson
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert.assertEquals
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

object SessionSpec : Spek({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()
    var context: ConfigurableApplicationContext? = null
    beforeGroup { context = SpringApplication.run(Application::class.java) }
    afterGroup { context!!.close() }

    describe("a session JSON endpoint") {

        it("should find the Dan North session") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/session/2421")
                    .flatMap { r -> r.bodyToFlux<Session>() })
                    .consumeNextWith {
                        assertEquals("Selling BDD to the Business", it.title)
                        assertEquals("NORTH", it.speakers.iterator().next().lastname)
                    }
                    .verifyComplete()
        }

        it("should find 27 sessions for mixit12") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/mixit12/session/")
                    .flatMap { it.bodyToFlux<Session>() })
                    .expectNextCount(27)
                    .verifyComplete()
        }
    }
})
