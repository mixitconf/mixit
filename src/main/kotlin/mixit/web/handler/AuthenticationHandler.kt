package mixit.web.handler

import mixit.MixitProperties
import mixit.model.User
import mixit.repository.UserRepository
import mixit.util.MailSender
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.toFormData
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.temporaryRedirect
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.util.*


@Component
class AuthenticationHandler(private val userRepository: UserRepository,
                            private val properties: MixitProperties,
                            private val mailSender: MailSender) {


    fun loginView(req: ServerRequest) = ok().render("login")

    fun login(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap { data ->
        req.session().flatMap { session ->
            val formData = data.toSingleValueMap()
            val email = formData["email"]

            if (email != null) {
//                // If user is an admin we go on the admin screen
//               if (email == properties.admin.email) {
//                    session.attributes["email"] = email
//                    if (session.isStarted) {
//                        session.maxIdleTime = Duration.ofHours(12)
//                    }
//                    //seeOther("${properties.baseUri}/admin")
//                } else {
                   findIfUserExist(email)
                           .log()
                            .flatMap { user ->
                                sendUserToken(email, user)
                            }
                           .thenEmpty(ok().render("auth", email))
                           .then(ok().render("auth", email))

//                            .thenEmpty {
//                                ok().render("user")
//                            }
//            }
        //        ok().render("login-error")
        }
        // If email don't exist we need more informations to open a session
        else {
                ok().render("login-error")
            }
    }
}

fun logout(req: ServerRequest): Mono<ServerResponse> = req.session().flatMap { session ->
    session.attributes.remove("email")
    temporaryRedirect(URI("${properties.baseUri}/")).build()
}


/**
 * Step 1 we need to find if a user exist.
 */
private fun findIfUserExist(email: String): Mono<User> = userRepository.findByEmail(email)

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
