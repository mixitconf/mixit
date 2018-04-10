package mixit.integration

import mixit.model.Role
import mixit.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.test.test


@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserIntegrationTests(@LocalServerPort port: Int) {

    private val client = WebClient.create("http://localhost:$port")

    @Test
    fun `Create a new user`() {
        client.post().uri("/api/user/").accept(APPLICATION_JSON).contentType(APPLICATION_JSON)
                .syncBody(User("brian", "Brian", "Clozel", "bc@gm.com"))
                .retrieve()
                .bodyToMono<User>()
                .test()
                .consumeNextWith { assertEquals("brian", it.login) }
                .verifyComplete()
    }

    @Test
    fun `Find Dan North`() {
        client.get().uri("/api/user/tastapod").accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono<User>()
                .test()
                .consumeNextWith {
                    assertEquals("North", it.lastname)
                    assertEquals("Dan", it.firstname)
                    assertTrue(it.role == Role.USER)
                }
                .verifyComplete()
    }

    @Test
    fun `Find Guillaume Ehret staff member`() {
        client.get().uri("/api/staff/guillaumeehret").accept(APPLICATION_JSON)
                .retrieve()
                .bodyToFlux<User>()
                .test()
                .consumeNextWith {
                    assertEquals("Ehret", it.lastname)
                    assertEquals("Guillaume", it.firstname)
                    assertTrue(it.role == Role.STAFF)
                }
                .verifyComplete()
    }

    @Test
    fun `Find all staff members`() {
        client.get().uri("/api/staff/").accept(APPLICATION_JSON)
                .retrieve()
                .bodyToFlux<User>()
                .test()
                .expectNextCount(7)
                .verifyComplete()
    }

    @Test
    fun `Find Zenika Lyon`() {
        client.get().uri("/api/user/ZenikaLyon").accept(APPLICATION_JSON)
                .retrieve()
                .bodyToFlux<User>()
                .test()
                .consumeNextWith {
                    assertEquals("Tournayre", it.lastname)
                    assertEquals("Louis", it.firstname)
                    assertTrue(it.role == Role.USER)
                }
                .verifyComplete()
    }

}
