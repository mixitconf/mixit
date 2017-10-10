package mixit.web.handler

import mixit.MixitProperties
import mixit.model.User
import mixit.repository.UserRepository
import mixit.util.MailSender
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.toFormData
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import java.net.URI
import java.time.LocalDateTime
import java.util.*


@Component
class AuthenticationHandler(private val userRepository: UserRepository,
                            private val properties: MixitProperties,
                            private val mailSender: MailSender) {


    fun loginView(req: ServerRequest) = ok().render("login")

    fun login(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap { data ->
        val formData = data.toSingleValueMap()
        val email = formData["email"]

        // Email is required
        if (email == null)  ok().render("login-error")

        val context = mapOf(Pair("email", email))

        // We need to know if user exists or not
        findIfUserExist(email!!)
                .flatMap { user ->
                    // if user exists we send a token by email
                    sendUserToken(email, user)
                            .flatMap { user ->
                                // if user is sent we call the the screen where user can type this token
                                ok().render("login-confirmation", context)
                            }
                            // if not this is an error
                            .switchIfEmpty(ServerResponse.badRequest().build());
                }
                // if user is not found we ask him if he wants to create a new account
                .switchIfEmpty(ServerResponse.ok().render("login-creation", context))
    }

    fun logout(req: ServerRequest): Mono<ServerResponse> = req.session().flatMap { session ->
        session.attributes.remove("email")
        temporaryRedirect(URI("${properties.baseUri}/")).build()
    }


    fun authenticate(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap { data ->
        val formData = data.toSingleValueMap()
        val email = formData["email"]
        val token = formData["token"]

        req.session().flatMap { session ->
            // If email or token are null we can't open a session
            if (email == null || token == null) {
                ok().render("login-error")
            }

            findIfUserExist(email!!)
                    // User must exist at this point
                    .flatMap { user ->
                        if (token == user.token) {
                            session.attributes["username"] = email
                            seeOther(URI("${properties.baseUri}/admin")).build()
                        }
                        ServerResponse.badRequest().build()
                    }
                    .switchIfEmpty(ServerResponse.badRequest().build());
        }
    }


    /**
     * Step 1 we need to find if a user exist.
     */
    private fun findIfUserExist(email: String): Mono<User> = userRepository.findByEmail(email).switchIfEmpty(Mono.empty())

    /**
     * Step 2 create a new user if he is not our database on step 1. If a user with the same mail is find we don't
     * create a new entry. If a user with the same lastname and firstname is find a confirmation is displayed to the
     * user
     */
    private fun createNewUser(user: User) =
            userRepository
                    .save(user)
                    .map {
                        val savedUser = it
                        userRepository
                                .findByName(user.firstname, user.lastname)
                                .then(Mono.just(UserCreated(user, CreationStatus.WARN_NAMES)))
                                .thenEmpty {
                                    Mono.just(UserCreated(savedUser, CreationStatus.OK))
                                }
                    }


    /**
     * Step 3 send a mail with a token to the user. We don't need validation of the email adress. If he receives
     * the email it's OK. If he retries a login a new token is sent
     */
    private fun sendUserToken(email: String, user: User): Mono<User> {
        user.token = UUID.randomUUID().toString()
        user.tokenExpiration = LocalDateTime.now().plusHours(12)
        mailSender.createEmail(email, "hello", "content")
        return userRepository.save(user)
    }

    /**
     * Step 4 receive the user token. He can click on the link sent by mail or copy and paste the token in the user
     * interface. If the token is valid a new session is opened for the user
     */
    private fun receiveUserToken() {}

}

data class UserCreated(
        val user: User,
        val status: CreationStatus
)

enum class CreationStatus {
    OK,
    WARN_NAMES
}
