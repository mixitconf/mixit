package mixit.integration

import mixit.model.Role
import mixit.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.client.exchange
import reactor.test.StepVerifier
import toMono


class UserIntegrationTests : AbstractIntegrationTests() {

    @Test
    fun `Create a new user`() {
        val user = client
                .post()
                .uri("/api/user/")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .exchange(User("brian", "Brian", "Clozel", "bc@gm.com").toMono())
                .flatMap { it.bodyToMono<User>() }

        StepVerifier.create(user)
                .consumeNextWith { assertEquals("brian", it.login) }
                .verifyComplete()
    }

    @Test
    fun `Find Dan North`() {
        val speaker = client
                .get()
                .uri("/api/speaker/tastapod")
                .accept(APPLICATION_JSON)
                .exchange()
                .then { r -> r.bodyToMono<User>() }

        StepVerifier.create(speaker)
                .consumeNextWith {
                    assertEquals("North", it.lastname)
                    assertEquals("Dan", it.firstname)
                    assertTrue(it.role == Role.SPEAKER)
                }
                .verifyComplete()
    }

    @Test
    fun `Find all MiXit 2015 speakers`() {
        val speakers = client
                .get()
                .uri("/api/mixit15/speaker/")
                .accept(APPLICATION_JSON).exchange()
                .flatMap { it.bodyToFlux<User>() }

        StepVerifier.create(speakers)
                .expectNextCount(60)
                .verifyComplete()
    }

    @Test
    fun `Find Guillaume Ehret staff member`() {
        val staffMember = client
                .get()
                .uri("/api/staff/guillaumeehret")
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap { it.bodyToFlux<User>() }

        StepVerifier.create(staffMember)
                .consumeNextWith {
                    assertEquals("Ehret", it.lastname)
                    assertEquals("Guillaume", it.firstname)
                    assertTrue(it.role == Role.STAFF)
                }
                .verifyComplete()
    }

    @Test
    fun `Find all staff members`() {
        val staffMembers = client
                .get()
                .uri("/api/staff/")
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap { it.bodyToFlux<User>() }

        StepVerifier.create(staffMembers)
                .expectNextCount(16)
                .verifyComplete()
    }

    @Test
    fun `Find Zenika Lyon sponsor`() {
        val sponsor = client
                .get()
                .uri("/api/sponsor/Zenika%20Lyon")
                .accept(APPLICATION_JSON)
                .exchange()
                .flatMap { it.bodyToFlux<User>() }

        StepVerifier.create(sponsor)
                .consumeNextWith {
                    assertEquals("Jacob", it.lastname)
                    assertEquals("Hervé", it.firstname)
                    assertTrue(it.role == Role.SPONSOR)
                }
                .verifyComplete()
    }

}
