package mixit.integration

import mixit.Application
import mixit.model.Staff
import mixit.support.getJson
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.subject.SubjectSpek
import org.junit.Assert.assertEquals
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

object StaffSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject { Application(SocketUtils.findAvailableTcpPort()) }
    beforeEachTest { subject.start() }
    afterEachTest { subject.stop() }

    describe("a staff JSON endpoint") {

        it("should find in staff Guillaume EHRET") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/staff/252")
                    .flatMap { it.bodyToFlux(Staff::class) })
                    .consumeNextWith {
                        assertEquals("EHRET", it.lastname)
                        assertEquals("Guillaume", it.firstname)
                    }
                    .verifyComplete()
        }

        it("should find 12 people in mixit staff") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/staff/")
                    .flatMap { it.bodyToFlux(Staff::class) })
                    .expectNextCount(12)
                    .verifyComplete()
        }
    }
})
