package mixit.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import java.time.LocalDateTime
import mixit.security.model.Cryptographer
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.jsonToken
import mixit.user.repository.UserRepository
import mixit.util.web.MixitWebFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MixitWebFilterITest(@Autowired val client: WebTestClient, @Autowired val cryptographer: Cryptographer) {

    @SpykBean
    private lateinit var userRepository: UserRepository

    fun anAdmin() = User("devmind", "Guillaume", "E", cryptographer.encrypt("guillaume@dev-mind.fr"), role = Role.STAFF, token = "token", tokenExpiration = LocalDateTime.now().plusDays(1))
    fun aUser() = User("tastapod", "Dan", "North", cryptographer.encrypt("dan@north.uk"), role = Role.USER, token = "token", tokenExpiration = LocalDateTime.now().plusDays(1))

    @Test
    fun `a robot even if it use a different locale is not redirected on english version`() {
        assertThat(
            client.get().uri("/")
                .header(HttpHeaders.USER_AGENT, "Google")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                // We are on the index
                .responseBody
        ).contains("/images/svg/mxt-icon--logo.svg")
    }

    @Test
    fun `an anonymous user should see an unsecured uri`() {
        client.get().uri("/api/talk/2421")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("\$.title").isEqualTo("Selling BDD to the Business")
            .jsonPath("\$.speakerIds").isEqualTo("tastapod")
    }

    @Test
    fun `an anonymous user should load a web resource`() {
        assertThat(
            client.get().uri("/images/svg/mxt-icon--heart.svg")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody
        ).isEqualTo("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"48.85\" height=\"43.33\" viewBox=\"0 0 48.85 43.33\"><title>mxt-icon--heart</title><g fill=\"#ff8d4e\" data-name=\"Layer 1\"><path d=\"M5.25 14.58a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H4.21a.22.22 0 0 1-.11-.06l-.1-.1-1.41-2.34-1.39 2.37-.06.08a.27.27 0 0 1-.1.06H.23a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L.1 8.93a.36.36 0 0 1-.1-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .24-.05H1.12l.1.05a.31.31 0 0 1 .06.08L2.66 11 4 8.74a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.7 2.79zM10.47 6.32a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H9.43a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.36L6.42 6.5l-.06.08a.27.27 0 0 1-.1.06h-.81a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16L7.1 3.45 5.32.68a.36.36 0 0 1-.06-.16.11.11 0 0 1 .06-.1.43.43 0 0 1 .19-.06H6.34l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L9.23.48a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17L8.6 3.41zM18.27 6.25a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H17.23a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.37-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.81-2.87L13.12.6a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H14.18l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L17 .41a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17L16.4 3.33zM26.38 14.37a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H25.34a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16L23 11.5l-1.76-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H22.26l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM33.49 6.1a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H32.45a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.37-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L28.35.45a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H29.37l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.2a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.78zM41.53 6a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H40.49a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.35-1.4 2.38-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L36.39.37a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H37.41l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L40.3.18a.26.26 0 0 1 .06-.08l.09-.1h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.84zM48.8 14.15a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06h-.79a.22.22 0 0 1-.11-.06l-.06-.08L46.15 12l-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.79-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H44.68l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM41.7 23.78a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H40.66a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.39 2.4-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.07-.13.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H37.58l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM33.76 33.35a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H32.72a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.79-2.75a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H29.63l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM26.66 43a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H25.62a.22.22 0 0 1-.11-.06l-.06-.08L24 40.76l-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H22.54l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17L24.79 40zM18.53 33.5a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H17.49a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.37-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H14.42l.1.05a.31.31 0 0 1 .06.08L16 29.88l1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM10.64 24.09a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H9.6a.22.22 0 0 1-.11-.06l-.06-.08L8 21.89l-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.81a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.73a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H6.52l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.81z\"/></g></svg>")
    }

    @Test
    fun `an anonymous user should be redirected if he used the old website url`() {
        assertThat(
            client.get().uri("http://mix-it.fr/api/talk/2421")
                .exchange()
                .expectStatus().is3xxRedirection
                .expectBody(String::class.java)
                .returnResult()
                .responseBody
        ).contains("<title>301 Moved Permanently</title>")
    }

    @Test
    fun `an anonymous user who launch home page with a locale different from france is redirected on english version`() {
        client.get().uri("/")
            .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5")
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/en/")
    }

    @Test
    fun `an anonymous user can not open a secured URL`() {
        client.get().uri("/me")
            .exchange()
            // User is redirected
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/login")
    }

    @Test
    fun `an anonymous user can not open an admin URL`() {
        client.get().uri("/admin")
            .exchange()
            // User is redirected
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/login")
    }

    @Test
    fun `a connected user should see an unsecured uri`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser())

        client.get().uri("/api/talk/2421")
            .cookie(MixitWebFilter.AUTENT_COOKIE, aUser().jsonToken(cryptographer))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("\$.title").isEqualTo("Selling BDD to the Business")
            .jsonPath("\$.speakerIds").isEqualTo("tastapod")
    }

    @Test
    fun `a connected user should load a web resource`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser())

        assertThat(
            client.get().uri("/images/svg/mxt-icon--heart.svg")
                .cookie(MixitWebFilter.AUTENT_COOKIE, aUser().jsonToken(cryptographer))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody
        ).isEqualTo("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"48.85\" height=\"43.33\" viewBox=\"0 0 48.85 43.33\"><title>mxt-icon--heart</title><g fill=\"#ff8d4e\" data-name=\"Layer 1\"><path d=\"M5.25 14.58a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H4.21a.22.22 0 0 1-.11-.06l-.1-.1-1.41-2.34-1.39 2.37-.06.08a.27.27 0 0 1-.1.06H.23a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L.1 8.93a.36.36 0 0 1-.1-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .24-.05H1.12l.1.05a.31.31 0 0 1 .06.08L2.66 11 4 8.74a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.7 2.79zM10.47 6.32a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H9.43a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.36L6.42 6.5l-.06.08a.27.27 0 0 1-.1.06h-.81a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16L7.1 3.45 5.32.68a.36.36 0 0 1-.06-.16.11.11 0 0 1 .06-.1.43.43 0 0 1 .19-.06H6.34l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L9.23.48a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17L8.6 3.41zM18.27 6.25a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H17.23a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.37-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.81-2.87L13.12.6a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H14.18l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L17 .41a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17L16.4 3.33zM26.38 14.37a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H25.34a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16L23 11.5l-1.76-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H22.26l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM33.49 6.1a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H32.45a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.37-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L28.35.45a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H29.37l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.2a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.78zM41.53 6a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H40.49a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.35-1.4 2.38-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L36.39.37a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H37.41l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L40.3.18a.26.26 0 0 1 .06-.08l.09-.1h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.84zM48.8 14.15a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06h-.79a.22.22 0 0 1-.11-.06l-.06-.08L46.15 12l-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.79-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H44.68l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM41.7 23.78a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H40.66a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.39 2.4-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.07-.13.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H37.58l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM33.76 33.35a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H32.72a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.79-2.75a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H29.63l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM26.66 43a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H25.62a.22.22 0 0 1-.11-.06l-.06-.08L24 40.76l-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H22.54l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17L24.79 40zM18.53 33.5a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H17.49a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.37-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H14.42l.1.05a.31.31 0 0 1 .06.08L16 29.88l1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM10.64 24.09a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H9.6a.22.22 0 0 1-.11-.06l-.06-.08L8 21.89l-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.81a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.73a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H6.52l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.81z\"/></g></svg>")
    }

    @Test
    fun `a connected user should be redirected if he used the old website url`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser())

        assertThat(
            client.get().uri("http://mix-it.fr/api/talk/2421")
                .cookie(MixitWebFilter.AUTENT_COOKIE, aUser().jsonToken(cryptographer))
                .exchange()
                .expectStatus().is3xxRedirection
                .expectBody(String::class.java)
                .returnResult()
                .responseBody
        ).contains("<title>301 Moved Permanently</title>")
    }

    @Test
    fun `a connected user who launch home page with a locale different from france is redirected on english version`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser())

        client.get().uri("/")
            .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5")
            .cookie(MixitWebFilter.AUTENT_COOKIE, aUser().jsonToken(cryptographer))
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/en/")
    }

    @Test
    fun `a connected user can open a secured URL`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser())

        assertThat(
            client.get().uri("/me")
                .cookie(MixitWebFilter.AUTENT_COOKIE, aUser().jsonToken(cryptographer))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                // User see his profile
                .responseBody
        ).contains("Dan North")
    }

    @Test
    fun `a connected user can not open a secured URL if his token is expired`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser().copy(tokenExpiration = LocalDateTime.now().minusMinutes(30)))

        client.get().uri("/me")
            .cookie(MixitWebFilter.AUTENT_COOKIE, aUser().jsonToken(cryptographer))
            .exchange()
            // User is redirected
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/login")
    }

    @Test
    fun `a connected user can not open an admin URL`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser())

        client.get().uri("/admin")
            .cookie(MixitWebFilter.AUTENT_COOKIE, aUser().jsonToken(cryptographer))
            .exchange()
            // User is redirected on home page
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/")
    }

    @Test
    fun `an admin user should see an unsecured uri`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(anAdmin())

        client.get().uri("/api/talk/2421")
            .cookie(MixitWebFilter.AUTENT_COOKIE, anAdmin().jsonToken(cryptographer))
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("\$.title").isEqualTo("Selling BDD to the Business")
            .jsonPath("\$.speakerIds").isEqualTo("tastapod")
    }

    @Test
    fun `an admin user should load a web resource`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(anAdmin())

        assertThat(
            client.get().uri("/images/svg/mxt-icon--heart.svg")
                .cookie(MixitWebFilter.AUTENT_COOKIE, anAdmin().jsonToken(cryptographer))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody
        ).isEqualTo("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"48.85\" height=\"43.33\" viewBox=\"0 0 48.85 43.33\"><title>mxt-icon--heart</title><g fill=\"#ff8d4e\" data-name=\"Layer 1\"><path d=\"M5.25 14.58a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H4.21a.22.22 0 0 1-.11-.06l-.1-.1-1.41-2.34-1.39 2.37-.06.08a.27.27 0 0 1-.1.06H.23a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L.1 8.93a.36.36 0 0 1-.1-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .24-.05H1.12l.1.05a.31.31 0 0 1 .06.08L2.66 11 4 8.74a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.7 2.79zM10.47 6.32a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H9.43a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.36L6.42 6.5l-.06.08a.27.27 0 0 1-.1.06h-.81a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16L7.1 3.45 5.32.68a.36.36 0 0 1-.06-.16.11.11 0 0 1 .06-.1.43.43 0 0 1 .19-.06H6.34l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L9.23.48a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17L8.6 3.41zM18.27 6.25a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H17.23a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.37-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.81-2.87L13.12.6a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H14.18l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L17 .41a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17L16.4 3.33zM26.38 14.37a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H25.34a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16L23 11.5l-1.76-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H22.26l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM33.49 6.1a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H32.45a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.37-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L28.35.45a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H29.37l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.2a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.78zM41.53 6a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H40.49a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.35-1.4 2.38-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92L36.39.37a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H37.41l.1.05a.31.31 0 0 1 .06.08l1.38 2.2L40.3.18a.26.26 0 0 1 .06-.08l.09-.1h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.84zM48.8 14.15a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06h-.79a.22.22 0 0 1-.11-.06l-.06-.08L46.15 12l-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.79-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H44.68l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM41.7 23.78a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H40.66a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.39 2.4-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.07-.13.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H37.58l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM33.76 33.35a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H32.72a.22.22 0 0 1-.11-.06l-.06-.08-1.45-2.35-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.79-2.75a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H29.63l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM26.66 43a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H25.62a.22.22 0 0 1-.11-.06l-.06-.08L24 40.76l-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H22.54l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17L24.79 40zM18.53 33.5a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H17.49a.22.22 0 0 1-.11-.06l-.06-.08-1.44-2.37-1.4 2.37-.06.08a.29.29 0 0 1-.1.06h-.79a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.78a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H14.42l.1.05a.31.31 0 0 1 .06.08L16 29.88l1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .1.1 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.71 2.78zM10.64 24.09a.32.32 0 0 1 .05.15.12.12 0 0 1-.06.1.47.47 0 0 1-.19.06H9.6a.22.22 0 0 1-.11-.06l-.06-.08L8 21.89l-1.4 2.37-.06.08a.27.27 0 0 1-.1.06h-.81a.44.44 0 0 1-.18-.05.12.12 0 0 1-.05-.1.33.33 0 0 1 .06-.16l1.82-2.92-1.78-2.73a.36.36 0 0 1-.06-.15.11.11 0 0 1 .06-.11.43.43 0 0 1 .19-.06H6.52l.1.05a.31.31 0 0 1 .06.08l1.38 2.2 1.35-2.23a.26.26 0 0 1 .06-.07l.09-.06h.76a.44.44 0 0 1 .18 0 .11.11 0 0 1 .05.1.42.42 0 0 1-.06.17l-1.72 2.81z\"/></g></svg>")
    }

    @Test
    fun `an admin user should be redirected if he used the old website url`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(anAdmin())

        assertThat(
            client.get().uri("http://mix-it.fr/api/talk/2421")
                .cookie(MixitWebFilter.AUTENT_COOKIE, anAdmin().jsonToken(cryptographer))
                .exchange()
                .expectStatus().is3xxRedirection
                .expectBody(String::class.java)
                .returnResult()
                .responseBody
        ).contains("<title>301 Moved Permanently</title>")
    }

    @Test
    fun `an admin user who launch home page with a locale different from france is redirected on english version`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(anAdmin())

        client.get().uri("/")
            .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5")
            .cookie(MixitWebFilter.AUTENT_COOKIE, anAdmin().jsonToken(cryptographer))
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/en/")
    }

    @Test
    fun `an admin user can open a secured URL`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(anAdmin())

        assertThat(
            client.get().uri("/me")
                .cookie(MixitWebFilter.AUTENT_COOKIE, anAdmin().jsonToken(cryptographer))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                // User see his profile
                .responseBody
        ).contains("Guillaume E")
    }

    @Test
    fun `an admin user can not open a secured URL if his token is expired`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(anAdmin().copy(tokenExpiration = LocalDateTime.now().minusMinutes(30)))

        client.get().uri("/me")
            .cookie(MixitWebFilter.AUTENT_COOKIE, anAdmin().jsonToken(cryptographer))
            .exchange()
            // User is redirected
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/login")
    }

    @Test
    fun `an admin user can not open an admin URL`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(anAdmin())

        assertThat(
            client.get().uri("/admin")
                .cookie(MixitWebFilter.AUTENT_COOKIE, anAdmin().jsonToken(cryptographer))
                .exchange()
                // User is redirected on home page
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                // User see his profile
                .responseBody
        ).contains("<h1 class=\"text-center pt-5\">Admin</h1>")
    }

    @Test
    fun `an admin user can not open an admin URL if his token is expired`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(anAdmin().copy(tokenExpiration = LocalDateTime.now().minusMinutes(30)))

        client.get().uri("/admin")
            .cookie(MixitWebFilter.AUTENT_COOKIE, anAdmin().jsonToken(cryptographer))
            .exchange()
            // User is redirected on home page
            .expectStatus().is3xxRedirection
            .expectHeader()
            .valueEquals(HttpHeaders.LOCATION, "http://localhost:8080/login")
    }
}
