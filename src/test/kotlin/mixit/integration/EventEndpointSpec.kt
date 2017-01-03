package mixit.integration

import mixit.Application
import mixit.model.Event
import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert.assertEquals
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientRequest.GET
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToFlux

class EventEndpointSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject {
        Application(port = SocketUtils.findAvailableTcpPort())
    }

    describe("an event JSON endpoint") {
        it("should find all 6 events from 2012 to 2017") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/event/").accept(APPLICATION_JSON_UTF8).build())
            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(Event::class)})
                    .consumeNextWith { assertEquals(2012, it.year) }
                    .consumeNextWith { assertEquals(2013, it.year) }
                    .consumeNextWith { assertEquals(2014, it.year) }
                    .consumeNextWith { assertEquals(2015, it.year) }
                    .consumeNextWith { assertEquals(2016, it.year) }
                    .consumeNextWith { assertEquals(2017, it.year) }
                    .expectComplete()
                    .verify()
            subject.stop()
        }
    }
