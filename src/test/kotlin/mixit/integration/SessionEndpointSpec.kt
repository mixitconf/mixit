package mixit.integration

import mixit.Application
import mixit.model.Session
import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert.assertEquals
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientRequest.GET
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

class SessionEndpointSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject {
        Application(port = SocketUtils.findAvailableTcpPort())
    }

    describe("a session JSON endpoint") {

        it("should find the Dan North session") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/mixit15/session/2421").accept(APPLICATION_JSON_UTF8).build())

            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(Session::class)})
                    .consumeNextWith { it ->
                        assertEquals("Selling BDD to the Business", it.title)
                        assertEquals("NORTH", it.speakers.iterator().next().lastname)
                    }
                    .expectComplete()
                    .verify()
            subject.stop()
        }

        it("should find 27 sessions for mixit12") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/mixit12/session/").accept(APPLICATION_JSON_UTF8).build())

            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(Session::class)})
                    .expectNextCount(27)
                    .expectComplete()
                    .verify()

            subject.stop()
        }
    }
})
