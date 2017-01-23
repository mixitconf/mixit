package mixit.integration

import mixit.model.Role
import mixit.model.User
import mixit.support.getHtml
import mixit.support.getJson
import mixit.support.postJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserIntegrationTests {

    @LocalServerPort
    lateinit var port: Integer

    val webClient = WebClient.builder(ReactorClientHttpConnector()).build()

    @Test
    fun `Create a new user`() {
        StepVerifier.create(webClient
                .postJson("http://localhost:$port/api/user/", User("brian", "Brian", "Clozel", "bc@gm.com"))
                .flatMap { it.bodyToMono<User>()} )
                .consumeNextWith { assertEquals("brian", it.login) }
                .verifyComplete()
    }

    @Test
    fun `Find Dan NORTH`() {
        StepVerifier.create(webClient
                .getJson("http://localhost:$port/api/speaker/tastapod")
                .then { r -> r.bodyToMono<User>()} )
                .consumeNextWith {
                    assertEquals("NORTH", it.lastname)
                    assertEquals("Dan", it.firstname)
                    assertTrue(it.role == Role.SPEAKER)
                }
                .verifyComplete()
    }

    @Test
    fun `Find all MiXit 2015 speakers`() {
        StepVerifier.create(webClient
                .getJson("http://localhost:$port/api/mixit15/speaker/")
                .flatMap { it.bodyToFlux<User>()} )
                .expectNextCount(60)
                .verifyComplete()
    }

    @Test
    fun `Find Guillaume Ehret staff member`() {
        StepVerifier.create(webClient
                .getJson("http://localhost:$port/api/staff/guillaumeehret")
                .flatMap { it.bodyToFlux<User>() })
                .consumeNextWith {
                    assertEquals("EHRET", it.lastname)
                    assertEquals("Guillaume", it.firstname)
                    assertTrue(it.role == Role.STAFF)
                }
                .verifyComplete()
    }

    @Test
    fun `Find all staff members`() {
        StepVerifier.create(webClient
                .getJson("http://localhost:$port/api/staff/")
                .flatMap { it.bodyToFlux<User>() })
                .expectNextCount(12)
                .verifyComplete()
    }

    @Test
    fun `Find Zenika Lyon sponsor`() {
        StepVerifier.create(webClient
                .getJson("http://localhost:$port/api/sponsor/Zenika%20Lyon")
                .flatMap { it.bodyToFlux<User>() })
                .consumeNextWith {
                    assertEquals("JACOB", it.lastname)
                    assertEquals("Hervé", it.firstname)
                    assertTrue(it.role == Role.SPONSOR)
                }
                .verifyComplete()
    }

    @Test
    fun `Find Joel SPOLSKY on users HTML page`() {
        StepVerifier.create(webClient
                .getHtml("http://localhost:$port/user/")
                .then { r -> r.bodyToMono<String>()} )
                .consumeNextWith { assertTrue(it.contains("Joel SPOLSKY")) }
                .verifyComplete()
    }

}
