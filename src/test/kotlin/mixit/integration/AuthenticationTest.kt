package mixit.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import mixit.model.Role
import mixit.model.User
import mixit.repository.UserRepository
import mixit.util.Cryptographer
import mixit.util.EmailService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationTest(@Autowired val client: WebTestClient,
                         @Autowired val cryptographer: Cryptographer) {

    @SpykBean
    private lateinit var userRepository: UserRepository

    @SpykBean
    private lateinit var emailService: EmailService

    fun aUser() = User("tastapod", "Dan", "North", cryptographer.encrypt("dan@north.uk"), role = Role.USER, token = "token", tokenExpiration = LocalDateTime.now().plusDays(1))


    @Test
    fun `should open the login page`() {
        val template = client.get().uri("/login").exchange().expectStatus().isOk.expectBody(String::class.java).returnResult().responseBody
        assertThat(template)
                .contains("<h1 class=\"text-center\">Authentification</h1>")
                // In the first step we don't ask for a token
                .doesNotContain("<input type=\"password\" name=\"token\" placeholder=\"Token\" />")
                // but only a password
                .contains("<input type=\"submit\" class=\"button expand\" value=\"Se connecter\"/>")
    }

    @Test
    fun `should not send a token when user send a wrong email`() {

        val template = client.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("email", "wrongemail"))
                .exchange()
                .expectStatus()
                .isOk.expectBody(String::class.java)
                .returnResult().responseBody

        assertThat(template).contains("Votre email est invalid")
    }

    @Test
    fun `should not send a token when user send nothing`() {

        val template = client.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("email", ""))
                .exchange()
                .expectStatus()
                .isOk.expectBody(String::class.java)
                .returnResult().responseBody

        assertThat(template).contains("Votre email est invalid")
    }

    @Test
    fun `should send a token when user send his email and when he is known in our user database`() {
        val slot = slot<User>()
        val user = aUser()
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(user)
        every { emailService.send("email-token", any(), any()) } answers {}
        every { userRepository.save(capture(slot)) } returns Mono.just(user)

        val template = client.post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("email", "dan@north.uk"))
                .exchange()
                .expectStatus()
                .isOk.expectBody(String::class.java)
                .returnResult().responseBody

        assertThat(template)
                .contains("<h1 class=\"text-center\">Authentification</h1>")
                // In the second step password is hidden
                .contains("<input type=\"hidden\" name=\"email\" value=\"dan&#64;north.uk\" />")
                // And we ask to send a token
                .contains("<input type=\"password\" name=\"token\" placeholder=\"Token\" />")

        assertThat(slot.captured.login).isEqualTo(user.login)
        assertThat(slot.captured.token).isNotEqualTo(user.token)
        assertThat(slot.captured.tokenExpiration).isNotEqualTo(user.tokenExpiration)

        verify { emailService.send("email-token", any(), any()) }
    }

    // TODO send a token

}