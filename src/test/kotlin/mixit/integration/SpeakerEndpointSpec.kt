package mixit.integration

import mixit.Application
import mixit.model.Speaker
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

class SpeakerEndpointSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject {
        Application(port = SocketUtils.findAvailableTcpPort())
    }

    describe("a speaker JSON endpoint") {

        it("should find in speaker Dan North") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/mixit15/speaker/2841").accept(APPLICATION_JSON_UTF8).build())

            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(Speaker::class)})
                    .consumeNextWith { it ->
                        assertEquals("NORTH", it.lastname)
                        assertEquals("Dan", it.firstname)
                    }
                    .expectComplete()
                    .verify()
            subject.stop()
        }

        it("should find 10 peaople in mixit speaker") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/mixit15/speaker/").accept(APPLICATION_JSON_UTF8).build())

            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(Speaker::class)})
                    .expectNextCount(60)
                    .expectComplete()
                    .verify()

            subject.stop()
        }
    }
})
