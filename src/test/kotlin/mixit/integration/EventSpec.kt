package mixit.integration

import mixit.Application
import mixit.model.Event
import mixit.support.getJson
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

object EventSpec : Spek({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()
    var context: ConfigurableApplicationContext? = null
    beforeGroup { context = SpringApplication.run(Application::class.java) }
    afterGroup { context!!.close() }

    describe("an event JSON endpoint") {

        it("should find mixit16") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/event/mixit16")
                    .then { r -> r.bodyToMono<Event>() })
                    .consumeNextWith {
                        assertEquals(2016, it.year)
                        assertFalse(it.current)
                    }
                    .verifyComplete()
        }

        it("should find 6 events from 2012 to 2017") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/event/")
                    .flatMap { it.bodyToFlux<Event>() })
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
