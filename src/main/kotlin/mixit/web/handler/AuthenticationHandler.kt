package mixit.web.handler

import mixit.MixitProperties
import mixit.model.Role
import mixit.model.User
import mixit.repository.UserRepository
import mixit.util.Cryptographer
import mixit.util.ElasticEmailSender
import mixit.util.locale
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
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
                            private val emailSender: ElasticEmailSender,
                            private val cryptographer: Cryptographer) {


    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Display a view with a form to send the email of the user
     */
    fun loginView(req: ServerRequest) = ok().render("login")

    /**
     * Action when user wants to sign in
     */
    fun login(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap { data ->
        // Email is required
        if (data.toSingleValueMap()["email"] == null) renderError("login.error.text")
        searchUserAndSendToken(data.toSingleValueMap()["email"]!!, req.locale())
    }

    /**
     * Action when user wants to log out
     */
    fun logout(req: ServerRequest): Mono<ServerResponse> = req.session().flatMap { session ->
        session.attributes.remove("email")
        session.attributes.remove("login")
        session.attributes.remove("token")
        session.attributes.remove("role")
        temporaryRedirect(URI("${properties.baseUri}/")).build()
    }

    private fun renderError(error: String?): Mono<ServerResponse> = ok().render("login-error", mapOf(Pair("description", error)))

    /**
     * Email is required for the sign in process. If email is not in our database, we ask to the visitor to create
     * a new account. If the account already exists we send him a token by email
     */
    private fun searchUserAndSendToken(email: String, locale: Locale): Mono<ServerResponse> {

        val context = mapOf(Pair("email", email))

        // We need to know if user exists or not
        return userRepository.findByEmail(email)
                .flatMap { user ->
                    // if user exists we send a token by email
                    sendUserToken(email, user, locale)
                            // if token is sent we call the the screen where user can type this token
                            .flatMap { ok().render("login-confirmation", context) }
                            // if not this is an error
                            .switchIfEmpty(renderError("login.error.sendtoken.text"))
                }
                // if user is not found we ask him if he wants to create a new account
                .switchIfEmpty(ServerResponse.ok().render("login-creation", context))
    }

    /**
     * Action to create a new account. Email, firstname, lastname are required in the form sent by the user
     */
    fun signUp(req: ServerRequest): Mono<ServerResponse> = req.body(BodyExtractors.toFormData()).flatMap {
        val formData = it.toSingleValueMap()

        if (formData["email"] == null || formData["firstname"] == null || formData["lastname"] == null)
            renderError("login.error.field.text")

        val email = formData["email"]!!
        val user = User(
                login = formData["email"]!!,
                firstname = formData["firstname"]!!.toLowerCase().capitalize(),
                lastname = formData["lastname"]!!.toLowerCase().capitalize(),
                email = email,
                photoUrl = "/images/png/mxt-icon--default-avatar.png",
                role = Role.USER
        )

        userRepository.findByEmail(email)
                // Email is unique and if an email is found we return an error
                .flatMap { renderError("login.error.uniqueemail.text") }
                .switchIfEmpty(
                        userRepository
                                .save(user)
                                // if user is created we send him a token by email
                                .flatMap { searchUserAndSendToken(email, req.locale()) }
                                // otherwise we display an error
                                .switchIfEmpty(renderError("login.error.creation.text"))
                )
    }

    /**
     * Action when user wants to send his token to open a session. This token is valid only for a limited time
     */
    fun signIn(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap { data ->
        val formData = data.toSingleValueMap()

        // If email or token are null we can't open a session
        if (formData["email"] == null || formData["token"] == null) {
            renderError("login.error.required.text")
        }

        val email = if (formData["email"]!!.contains("@")) formData["email"] else cryptographer.decrypt(formData["email"])
        val token = formData["token"]

        req.session().flatMap { session ->
            userRepository.findByEmail(email!!)
                    // User must exist at this point
                    .flatMap { user ->
                        if (token!!.trim() == user.token) {
                            if (user.tokenExpiration.isBefore(LocalDateTime.now())) {
                                // token has to be valid
                                renderError("login.error.token.text")
                            } else {
                                session.attributes["role"] = user.role
                                session.attributes["email"] = email
                                session.attributes["token"] = token
                                seeOther(URI("${properties.baseUri}/")).build()
                            }
                        } else {
                            renderError("login.error.badtoken.text")
                        }

                    }
                    .switchIfEmpty(renderError("login.error.bademail.text"))
        }
    }

    /**
     * Sends an email with a token to the user. We don't need validation of the email adress. If he receives
     * the email it's OK. If he retries a login a new token is sent
     */
    private fun sendUserToken(email: String, user: User, locale: Locale): Mono<User> {
        val userToUpdate = User(
                user.login,
                user.firstname,
                user.lastname,
                cryptographer.encrypt(email),
                user.company,
                user.description,
                user.emailHash,
                user.photoUrl,
                user.role,
                user.links,
                user.legacyId,
                LocalDateTime.now().plusHours(12),
                UUID.randomUUID().toString())

        try {
            logger.info("A token was sent to $email")
            emailSender.sendUserTokenEmail(userToUpdate, locale)
            return userRepository.save(userToUpdate)
        } catch (e: RuntimeException) {
            logger.error(e.message, e)
            return Mono.empty()
        }
    }
}
