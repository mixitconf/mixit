package mixit.integration

import mixit.Application
import mixit.model.Event
import mixit.support.getJson
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.subject.SubjectSpek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToMono
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

object EventSpec : SubjectSpek<Application>({

    subject { Application(SocketUtils.findAvailableTcpPort()) }
    beforeEachTest { subject.start() }
    afterEachTest { subject.stop() }

    describe("an event JSON endpoint") {

        val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

        it("should find mixit16") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/event/mixit16")
                    .then { r -> r.bodyToMono(Event::class) })
                    .consumeNextWith {
                        assertEquals(2016, it.year)
                        assertFalse(it.current)
                    }
                    .verifyComplete()
        }

        it("should find 6 events from 2012 to 2017") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/event/")
                    .flatMap { it.bodyToFlux(Event::class) })
                    .consumeNextWith { assertEquals(2012, it.year) }
                    .consumeNextWith { assertEquals(2013, it.year) }
                    .consumeNextWith { assertEquals(2014, it.year) }
                    .consumeNextWith { assertEquals(2015, it.year) }
                    .consumeNextWith { assertEquals(2016, it.year) }
                    .consumeNextWith { assertEquals(2017, it.year) }
                    .verifyComplete()
        }
    }
})
