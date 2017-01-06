package mixit.integration

import mixit.Application
import mixit.model.Staff
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

class StaffEndpointSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject {
        Application(port = SocketUtils.findAvailableTcpPort())
    }

    describe("a staff JSON endpoint") {

        it("should find in staff Guillaume EHRET") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/staff/252").accept(APPLICATION_JSON_UTF8).build())

            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(Staff::class)})
                    .consumeNextWith { it ->
                        assertEquals("EHRET", it.lastname)
                        assertEquals("Guillaume", it.firstname)
                    }
                    .expectComplete()
                    .verify()
            subject.stop()
        }

        it("should find 10 peaople in mixit staff") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/staff/").accept(APPLICATION_JSON_UTF8).build())

            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(Staff::class)})
                    .expectNextCount(12)
                    .expectComplete()
                    .verify()

            subject.stop()
        }
    }
})
