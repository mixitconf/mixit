package mixit.integration

import mixit.Application
import mixit.model.Role
import mixit.model.User
import mixit.support.getHtml
import mixit.support.getJson
import mixit.support.postJson
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.subject.SubjectSpek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToFlux
import org.springframework.web.reactive.function.client.ClientResponseExtension.bodyToMono
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

object UserSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject { Application(SocketUtils.findAvailableTcpPort()) }
    beforeEachTest { subject.start() }
    afterEachTest { subject.stop() }

    describe("a user JSON endpoint") {

        it("should insert a new user Brian") {
            StepVerifier.create(webClient
                    .postJson("http://localhost:${subject.port}/api/user/", User("brian", "Brian", "Clozel", "bc@gm.com"))
                    .flatMap { it.bodyToMono(User::class)} )
                    .consumeNextWith { assertEquals("brian", it.login) }
                    .verifyComplete()
        }

         it("should find speaker Dan North") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/speaker/tastapod")
                    .then { r -> r.bodyToMono(User::class)} )
                    .consumeNextWith {
                        assertEquals("NORTH", it.lastname)
                        assertEquals("Dan", it.firstname)
                        assertTrue(it.role == Role.SPEAKER)
                    }
                    .verifyComplete()
        }

        it("should find 60 people in Mixit 15 speakers") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/mixit15/speaker/")
                    .flatMap { it.bodyToFlux(User::class)} )
                    .expectNextCount(60)
                    .verifyComplete()
        }

         it("should find staff member Guillaume EHRET") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/staff/guillaumeehret")
                    .flatMap { it.bodyToFlux(User::class) })
                    .consumeNextWith {
                        assertEquals("EHRET", it.lastname)
                        assertEquals("Guillaume", it.firstname)
                        assertTrue(it.role == Role.STAFF)
                    }
                    .verifyComplete()
        }

        it("should find 12 people in mixit staff") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/staff/")
                    .flatMap { it.bodyToFlux(User::class) })
                    .expectNextCount(12)
                    .verifyComplete()
        }

        it("should find Hervé JACOB") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/sponsor/Zenika%20Lyon")
                    .flatMap { it.bodyToFlux(User::class) })
                    .consumeNextWith {
                        assertEquals("JACOB", it.lastname)
                        assertEquals("Hervé", it.firstname)
                        assertTrue(it.role == Role.SPONSOR)
                    }
                    .verifyComplete()
        }

    }

    describe("a user list web page") {

        it("should contain 3 users Robert, Raide and Ford") {
            StepVerifier.create(webClient
                    .getHtml("http://localhost:${subject.port}/user/")
                    .then { r -> r.bodyToMono(String::class)} )
                    .consumeNextWith { it.contains("Robert") && it.contains("Raide") && it.contains("Ford") }
                    .verifyComplete()
        }
    }
})
