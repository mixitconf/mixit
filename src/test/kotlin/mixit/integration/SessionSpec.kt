package mixit.integration

import mixit.Application
import mixit.model.Session
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

object SessionSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject { Application(SocketUtils.findAvailableTcpPort()) }
    beforeEachTest { subject.start() }
    afterEachTest { subject.stop() }

    describe("a session JSON endpoint") {

        it("should find the Dan North session") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/mixit15/session/2421")
                    .flatMap { r -> r.bodyToFlux(Session::class) })
                    .consumeNextWith {
                        assertEquals("Selling BDD to the Business", it.title)
                        assertEquals("NORTH", it.speakers.iterator().next().lastname)
                    }
                    .verifyComplete()
        }

        it("should find 27 sessions for mixit12") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/mixit12/session/")
                    .flatMap { it.bodyToFlux(Session::class) })
                    .expectNextCount(27)
                    .verifyComplete()
        }
    }
})
