package mixit.integration

import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.verify
import mixit.model.Role
import mixit.model.User
import mixit.util.encodeToBase64
import mixit.web.service.AuthenticationService
import mixit.web.service.DuplicateException
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
class AuthenticationTest(@Autowired val client: WebTestClient) {

    @SpykBean
    lateinit var authenticationService: AuthenticationService

    val aUser = User("tastapod", "Dan", "North", "dan@north.uk", role = Role.USER, token = "token", tokenExpiration = LocalDateTime.now().plusDays(1))

    @Test
    fun `should open the login page`() {
        val result = client.get().uri("/login")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // A field to send email
            .contains("<input type=\"text\" class=\"form-control\" name=\"email\" placeholder=\"contact@mix-it.fr\" />")
            // A button to log in
            .contains(">Se connecter<")
    }

    @Test
    fun `should open the login page with token when the user click on the link received by email`() {
        val result = client.get().uri("/signin/${"my-token"}/${"dan@north.uk".encodeToBase64()}")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // An hidden field to send email
            .contains("<input type=\"hidden\" name=\"email\" value=\"dan&#64;north.uk\"/>")
            // A field to send token
            .contains("<input type=\"password\" class=\"form-control\" name=\"token\" placeholder=\"Token\" value=\"my-token\"/>")
            // A button to log in
            .contains("class=\"nav-link mxt-nav-link\">Se connecter</a>")
    }

    @Test
    fun `should send a token when a user send his email on the login page`() {
        every { authenticationService.searchUserByEmailOrCreateHimFromTicket(any()) } returns Mono.just(aUser)
        every { authenticationService.generateAndSendToken(any(), any()) } returns Mono.just(aUser)

        val result = client.post().uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", "dan@north.uk"))
            .exchange()
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // A message to say that a token was sent
            .contains(
                "Un token de connexion vient de vous être envoyé par email. Vous pouvez soit utiliser le " +
                    "lien contenu dans ce mail pour vous connecter, soit coller dans le champ correspondant le " +
                    "token reçu. "
            )
            // A field to copy this token
            .contains("<input type=\"password\" class=\"form-control\" name=\"token\" placeholder=\"Token\" />")
            // A button to send informations
            .contains("<button type=\"submit\" class=\"btn btn-primary mxt-btn-primary\">Se connecter</button>")

        verify { authenticationService.searchUserByEmailOrCreateHimFromTicket(any()) }
        verify { authenticationService.generateAndSendToken(any(), any()) }
    }

    @Test
    fun `should display error page when a user send an invalid email on the login page`() {
        val result = client.post().uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", "dannorth.uk"))
            .exchange()
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // An error message
            .contains("Votre email est invalide")
            // A button to go back
            .contains("<p><a href=\"/login\">Retour vers le formulaire</a></p>")
    }

    @Test
    fun `should display error page when a token can't be send because email service failed`() {
        every { authenticationService.searchUserByEmailOrCreateHimFromTicket(any()) } returns Mono.just(aUser)
        every { authenticationService.generateAndSendToken(any(), any()) } returns Mono.error { DuplicateException("Login duplicated") }

        val result = client.post().uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", "dan@north.uk"))
            .exchange()
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // An error message
            .contains("Erreur lors de l'envoi du mail contenant le token de connexion. Essayez une nouvelle fois ou contacter nous")
            // A button to go back
            .contains("<p><a href=\"/login\">Retour vers le formulaire</a></p>")

        verify { authenticationService.searchUserByEmailOrCreateHimFromTicket(any()) }
        verify { authenticationService.generateAndSendToken(any(), any()) }
    }

    @Test
    fun `should ask more informations to a user to create his account after he send his email on the login page`() {
        val result = client.post().uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", "dan@north.uk"))
            .exchange()
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // a message to alert user
            .contains("Votre e-mail n'est pas associé à un compte. Si vous voulez créer un compte, donnez nous un peu plus de détail ")
            // an hidden field with user email
            .contains("<input type=\"hidden\" name=\"email\" value=\"dan&#64;north.uk\"/>")
            // firstname
            .contains("<input type=\"text\" id=\"firstname\" name=\"firstname\" class=\"form-control\" required/>")
            // lastname
            .contains("<input type=\"text\" id=\"lastname\" class=\"form-control\" name=\"lastname\" required/>")
            // a button to create an account
            .contains("<button type=\"submit\" class=\"btn btn-primary mxt-btn-primary\">Créer un compte</button>")
    }

    @Test
    fun `should create user and him send a cookie to the user if his email and token are valids`() {
        // every { authenticationService.createUser(any(), any(), any()) } returns Pair("dan@north.uk", aUser)
        every { authenticationService.createUserIfEmailDoesNotExist(any(), any()) } returns Mono.just(aUser)
        every { authenticationService.generateAndSendToken(any(), any()) } returns Mono.just(aUser)

        val result = client.post().uri("/signup")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("email", "dan@north.uk")
                    .with("firstname", "dan")
                    .with("lastname", "north")
            )
            .exchange()
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // A message to say that a token was sent
            .contains(
                "Un token de connexion vient de vous être envoyé par email. Vous pouvez soit utiliser le " +
                    "lien contenu dans ce mail pour vous connecter, soit coller dans le champ correspondant le " +
                    "token reçu. "
            )
            // A field to copy this token
            .contains("<input type=\"password\" class=\"form-control\" name=\"token\" placeholder=\"Token\" />")
            // A button to send informations
            .contains("<button type=\"submit\" class=\"btn btn-primary mxt-btn-primary\">Se connecter</button>")

        verify { authenticationService.createUserIfEmailDoesNotExist(any(), any()) }
        verify { authenticationService.generateAndSendToken(any(), any()) }
    }

    @Test
    fun `should not create new user if firstname or lastname are missing`() {
        // every { authenticationService.createUser(any(), any(), any()) } returns ("dan@north.uk", aUser)
        every { authenticationService.createUserIfEmailDoesNotExist(any(), any()) } returns Mono.just(aUser)
        every { authenticationService.generateAndSendToken(any(), any()) } returns Mono.just(aUser)

        val result = client.post().uri("/signup")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("email", "dan@north.uk"))
            .exchange()
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // A message to say that a token was sent
            .contains("Tous les champs sont requis.")
            // A button to go back
            .contains("Retour vers le formulaire")
    }

    @Test
    fun `should not create new user if email is invalid`() {
        // every { authenticationService.createUser(any(), any(), any()) } returns ("dan@north.uk", aUser)
        every { authenticationService.createUserIfEmailDoesNotExist(any(), any()) } returns Mono.just(aUser)
        every { authenticationService.generateAndSendToken(any(), any()) } returns Mono.just(aUser)

        val result = client.post().uri("/signup")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters.fromFormData("email", "dannorth.uk")
                    .with("firstname", "dan")
                    .with("lastname", "north")
            )
            .exchange()
            .expectBody(String::class.java)
            .returnResult().responseBody

        assertThat(result)
            .contains("<h1 class=\"text-center pt-5\">Authentification</h1>")
            // A message to say that a token was sent
            .contains("Votre email est invalide")
            // A button to go back
            .contains("<p><a href=\"/login\">Retour vers le formulaire</a></p>")
    }
}
