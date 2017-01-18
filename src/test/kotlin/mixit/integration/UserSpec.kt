package mixit.integration

import mixit.Application
import mixit.model.Role
import mixit.model.User
import mixit.support.getHtml
import mixit.support.getJson
import mixit.support.postJson
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

object UserSpec : Spek({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()
    var context: ConfigurableApplicationContext? = null
    beforeGroup { context = SpringApplication.run(Application::class.java) }
    afterGroup { SpringApplication.exit(context) }

    describe("a user endpoint") {

        it("should insert a new user Brian") {
            StepVerifier.create(webClient
                    .postJson("http://localhost:8080/api/user/", User("brian", "Brian", "Clozel", "bc@gm.com"))
                    .flatMap { it.bodyToMono<User>()} )
                    .consumeNextWith { assertEquals("brian", it.login) }
                    .verifyComplete()
        }

         it("should find speaker Dan North") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/speaker/tastapod")
                    .then { r -> r.bodyToMono<User>()} )
                    .consumeNextWith {
                        assertEquals("NORTH", it.lastname)
                        assertEquals("Dan", it.firstname)
                        assertTrue(it.role == Role.SPEAKER)
                    }
                    .verifyComplete()
        }

        it("should find 60 people in Mixit 15 speakers") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/mixit15/speaker/")
                    .flatMap { it.bodyToFlux<User>()} )
                    .expectNextCount(60)
                    .verifyComplete()
        }

         it("should find staff member Guillaume EHRET") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/staff/guillaumeehret")
                    .flatMap { it.bodyToFlux<User>() })
                    .consumeNextWith {
                        assertEquals("EHRET", it.lastname)
                        assertEquals("Guillaume", it.firstname)
                        assertTrue(it.role == Role.STAFF)
                    }
                    .verifyComplete()
        }

        it("should find 12 people in mixit staff") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/staff/")
                    .flatMap { it.bodyToFlux<User>() })
                    .expectNextCount(12)
                    .verifyComplete()
        }

        it("should find Hervé JACOB") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:8080/api/sponsor/Zenika%20Lyon")
                    .flatMap { it.bodyToFlux<User>() })
                    .consumeNextWith {
                        assertEquals("JACOB", it.lastname)
                        assertEquals("Hervé", it.firstname)
                        assertTrue(it.role == Role.SPONSOR)
                    }
                    .verifyComplete()
        }

//         it("page should contain 3 users Robert, Raide and Ford") {
//            StepVerifier.create(webClient
//                    .getHtml("http://localhost:8080/user/")
//                    .then { r -> r.bodyToMono<String>()} )
//                    .consumeNextWith { it.contains("Robert") && it.contains("Raide") && it.contains("Ford") }
//                    .verifyComplete()
//        }

    }

})
