package mixit.integration

import mixit.Application
import mixit.model.Speaker
import mixit.support.getJson
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.subject.SubjectSpek
import org.junit.Assert.assertEquals
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToMono
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

object SpeakerSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject { Application(SocketUtils.findAvailableTcpPort()) }
    beforeEachTest { subject.start() }
    afterEachTest { subject.stop() }

    describe("a speaker JSON endpoint") {

        it("should find speaker Dan North") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/mixit15/speaker/2841")
                    .then { r -> r.bodyToMono(Speaker::class)} )
                    .consumeNextWith {
                        assertEquals("NORTH", it.lastname)
                        assertEquals("Dan", it.firstname)
                    }
                    .verifyComplete()
        }

        it("should find 60 people in Mixit 15 speakers") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/mixit15/speaker/")
                    .flatMap { it.bodyToFlux(Speaker::class)} )
                    .expectNextCount(60)
                    .verifyComplete()
        }
    }
})
