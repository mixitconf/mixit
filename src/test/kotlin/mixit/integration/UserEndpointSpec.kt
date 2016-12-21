package mixit.integration

import mixit.Application
import mixit.model.User
import mixit.support.bodyToFlux
import mixit.support.bodyToMono
import org.jetbrains.spek.api.SubjectSpek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.util.SocketUtils
import org.springframework.web.reactive.function.client.ClientRequest.GET
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

class UserEndpointSpec : SubjectSpek<Application>({

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    subject {
        Application(port = SocketUtils.findAvailableTcpPort())
    }

    describe("a user JSON endpoint") {
        it("should find 3 users Robert, Raide and Ford when requesting '/api/user/'") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/api/user/").accept(APPLICATION_JSON_UTF8).build())
            StepVerifier.create(response.flatMap{ r -> r.bodyToFlux(User::class)})
                    .consumeNextWith { assert(it == User(1L, "Robert")) }
                    .consumeNextWith { assert(it == User(2L, "Raide"))  }
                    .consumeNextWith { assert(it == User(3L, "Ford")) }
                    .expectComplete()
                    .verify()
            subject.stop()
        }
    }

    describe("a user list web page") {
        it("should contain 3 users Robert, Raide and Ford") {
            subject.start()
            val response = webClient.exchange(GET("http://localhost:${subject.port}/user/").accept(TEXT_HTML).build())
            StepVerifier.create(response.then{ r -> r.bodyToMono(String::class)})
                    .consumeNextWith { it.contains("Robert") && it.contains("Raide") && it.contains("Ford") }
                    .expectComplete()
                    .verify()
            subject.stop()
        }
    }
})