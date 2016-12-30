package mixit.integration

import mixit.Application
import mixit.model.sponsor.Event
import mixit.support.bodyToFlux
import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientRequest.GET
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import java.time.Instant

@RunWith(JUnitPlatform::class)
class EventEndpointSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject {
        Application(port = SocketUtils.findAvailableTcpPort())
    }

    describe("an event JSON endpoint") {
        it("should find 6 events '/api/event/'") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/event/").accept(APPLICATION_JSON_UTF8).build())
            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(Event::class)})
                    .consumeNextWith { assert(it == Event(1L, 2012, false, Instant.ofEpochMilli(1461650400000), Instant.ofEpochMilli(1461686400000))) }
                    //.expectNextCount(5)
                    .expectComplete()
                    .verify()
            subject.stop()
        }
    }

})