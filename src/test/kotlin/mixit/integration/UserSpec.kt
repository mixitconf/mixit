package mixit.integration

import mixit.Application
import mixit.model.User
import mixit.support.getHtml
import mixit.support.getJson
import mixit.support.postJson
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.subject.SubjectSpek
import org.junit.Assert.assertEquals
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.client.ClientRequest.GET
import org.springframework.web.reactive.function.client.ClientRequest.POST
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

        it("should find all 3 users Robert, Raide and Ford") {
            StepVerifier.create(webClient
                    .getJson("http://localhost:${subject.port}/api/user/")
                    .flatMap { it.bodyToFlux(User::class)} )
                    .consumeNextWith { assertEquals("robert", it.id) }
                    .consumeNextWith { assertEquals("raide", it.id) }
                    .consumeNextWith { assertEquals("ford", it.id) }
                    .verifyComplete()
        }

        it("should insert a new user Brian") {
            StepVerifier.create(webClient
                    .postJson("http://localhost:${subject.port}/api/user/", User("brian", "Brian", "Clozel"))
                    .flatMap { it.bodyToMono(User::class)} )
                    .consumeNextWith { assertEquals("brian", it.id) }
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
