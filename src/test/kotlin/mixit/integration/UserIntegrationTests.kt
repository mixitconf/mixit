package mixit.integration

import mixit.model.Role
import mixit.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.exchange
import reactor.test.StepVerifier
import toMono


class UserIntegrationTests : AbstractIntegrationTests() {

    @Test
    fun `Create a new user`() {
        StepVerifier.create(client.post().uri("http://localhost:$port/api/user/")
                .accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
                .exchange(User("brian", "Brian", "Clozel", "bc@gm.com").toMono())
                .flatMap { it.bodyToMono<User>()} )
                .consumeNextWith { assertEquals("brian", it.login) }
                .verifyComplete()
    }

    @Test
    fun `Find Dan NORTH`() {
        StepVerifier.create(client.get().uri("http://localhost:$port/api/speaker/tastapod")
                .accept(APPLICATION_JSON).exchange()
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
        StepVerifier.create(client.get().uri("http://localhost:$port/api/mixit15/speaker/")
                .accept(APPLICATION_JSON).exchange()
                .flatMap { it.bodyToFlux<User>()} )
                .expectNextCount(60)
                .verifyComplete()
    }

    @Test
    fun `Find Guillaume Ehret staff member`() {
        StepVerifier.create(client.get().uri("http://localhost:$port/api/staff/guillaumeehret")
                .accept(APPLICATION_JSON).exchange()
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
        StepVerifier.create(client.get().uri("http://localhost:$port/api/staff/")
                .accept(APPLICATION_JSON).exchange()
                .flatMap { it.bodyToFlux<User>() })
                .expectNextCount(12)
                .verifyComplete()
    }

    @Test
    fun `Find Zenika Lyon sponsor`() {
        StepVerifier.create(client.get().uri("http://localhost:$port/api/sponsor/Zenika%20Lyon")
                .accept(APPLICATION_JSON).exchange()
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
        StepVerifier.create(client.get().uri("http://localhost:$port/user/")
                .accept(TEXT_HTML).exchange()
                .then { r -> r.bodyToMono<String>()} )
                .consumeNextWith { assertTrue(it.contains("Joel SPOLSKY")) }
                .verifyComplete()
    }

}
