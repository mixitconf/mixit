package mixit.web.handler

import mixit.MixitProperties
import mixit.model.Role
import mixit.model.User
import mixit.repository.TicketRepository
import mixit.repository.UserRepository
import mixit.util.*
import mixit.util.validator.EmailValidator
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyExtractors.toFormData
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import java.net.URI
import java.net.URLDecoder
import java.time.Duration
import java.time.LocalDateTime
import java.util.*


@Component
class AuthenticationHandler(private val userRepository: UserRepository,
                            private val ticketRepository: TicketRepository,
                            private val properties: MixitProperties,
                            private val emailService: EmailService,
                            private val emailValidator: EmailValidator,
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
        if (data.toSingleValueMap()["email"] == null) {
            renderError("login.error.text")
        } else if (!emailValidator.isValid(data.toSingleValueMap()["email"]!!)) {
            renderError("login.error.creation.mail")
        } else {
            searchUserAndSendToken(req, data.toSingleValueMap()["email"]!!.trim().toLowerCase())
        }
    }

    /**
     * Action when user wants to log out
     */
    fun logout(req: ServerRequest): Mono<ServerResponse> = req.session().flatMap { session ->
        Mono.justOrEmpty(session.getAttribute<String>("email"))
                .flatMap {
                    userRepository
                            .findByEmail(it)
                            .flatMap {
                                updateUserToken(it, req.locale())
                                        .flatMap { clearSession(session) }
                                        .switchIfEmpty(clearSession(session))
                            }
                            .switchIfEmpty(clearSession(session))
                }
                .switchIfEmpty(clearSession(session))
    }

    private fun clearSession(session: WebSession): Mono<ServerResponse> {
        session.attributes.remove("email")
        session.attributes.remove("login")
        session.attributes.remove("token")
        session.attributes.remove("role")
        return temporaryRedirect(URI("${properties.baseUri}/")).build()
    }

    private fun renderError(error: String?): Mono<ServerResponse> = ok().render("login-error", mapOf(Pair("description", error)))

    /**
     * Email is required for the sign in process. If email is not in our database, we ask to the visitor to create
     * a new account. If the account already exists we send him a token by email
     */
    private fun searchUserAndSendToken(req: ServerRequest, email: String): Mono<ServerResponse> {

        val context = mapOf(Pair("email", email))

        // We need to know if user exists or not
        return userRepository.findByEmail(email)
                .flatMap { user ->
                    // if user exists we send a token by email
                    updateUserToken(if (user.email == null) user.updateEmail(cryptographer, email) else user, req.locale(), sendToken = true)
                            // if token is sent we call the the screen where user can type this token
                            .flatMap { ok().render("login-confirmation", context) }
                            // if not this is an error
                            .switchIfEmpty(renderError("login.error.sendtoken.text"))
                }
                // if user is not found we ask him if he wants to create a new account
                .switchIfEmpty(searchUserInTicketsAndSendToken(req, email, context))
    }


    private fun searchUserInTicketsAndSendToken(req: ServerRequest, email: String, context: Map<String, String>): Mono<ServerResponse> =
            ticketRepository.findByEmail(email)
                .flatMap { ticket ->
                    createUser(
                            req,
                            email,
                            User(
                                    login = email.split("@").get(0),
                                    firstname = ticket.firstname.toLowerCase().capitalize(),
                                    lastname = ticket.lastname.toLowerCase().capitalize(),
                                    email = cryptographer.encrypt(email),
                                    photoUrl = "/images/png/mxt-icon--default-avatar.png",
                                    role = Role.USER
                            )
                    )
                }
                // if user is not found we ask him if he wants to create a new account
                .switchIfEmpty(ServerResponse.ok().render("login-creation", context))

    /**
     * Action to create a new account. Email, firstname, lastname are required in the form sent by the user
     */
    fun signUp(req: ServerRequest): Mono<ServerResponse> = req.body(BodyExtractors.toFormData()).flatMap {
        val formData = it.toSingleValueMap()

        if (formData["email"] == null || formData["firstname"] == null || formData["lastname"] == null)
            renderError("login.error.field.text")

        val email = formData["email"]!!.trim().toLowerCase()
        val user = User(
                login = email.split("@").get(0),
                firstname = formData["firstname"]!!.toLowerCase().capitalize(),
                lastname = formData["lastname"]!!.toLowerCase().capitalize(),
                email = cryptographer.encrypt(email),
                photoUrl = "/images/png/mxt-icon--default-avatar.png",
                role = Role.USER
        )

        if (!emailValidator.isValid(email)) {
            renderError("login.error.creation.mail")
        } else {
            createUser(req, email, user)
        }
    }

    fun createUser(req: ServerRequest, nonEncryptedEmail: String, user: User): Mono<ServerResponse> = userRepository
            .findByEmail(nonEncryptedEmail)
            // Email is unique and if an email is found we return an error
            .flatMap { renderError("login.error.uniqueemail.text") }
            .switchIfEmpty(
                    userRepository
                            .save(user)
                            // if user is created we send him a token by email
                            .flatMap { searchUserAndSendToken(req, nonEncryptedEmail.trim().toLowerCase()) }
                            // otherwise we display an error
                            .switchIfEmpty(renderError("login.error.creation.text"))
            )


    /**
     * Action when user wants to send his token to open a session. This token is valid only for a limited time
     * This action is launched when user clicks on the link sent by email
     */
    fun signInViaUrl(req: ServerRequest): Mono<ServerResponse> {
        val email = URLDecoder.decode(req.pathVariable("email"), "UTF-8").decodeFromBase64()
        val token = req.pathVariable("token")

        val context = mapOf(Pair("email", email), Pair("token", token))
        return ok().render("login-confirmation", context)
    }


    /**
     * Action when user wants to send his token to open a session. This token is valid only for a limited time
     * This action is launched when user copy the token in the website
     */
    fun signIn(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap { data ->
        val formData = data.toSingleValueMap()

        // If email or token are null we can't open a session
        if (formData["email"] == null || formData["token"] == null) {
            renderError("login.error.required.text")
        }

        val email = if (formData["email"]!!.contains("@")) formData["email"] else cryptographer.decrypt(formData["email"])
        val token = formData["token"]!!

        req.session().flatMap { session ->
            userRepository.findByEmail(email!!)
                    // User must exist at this point
                    .flatMap { user ->
                        if (token.trim() == user.token) {
                            if (user.tokenExpiration.isBefore(LocalDateTime.now())) {
                                // token has to be valid
                                renderError("login.error.token.text")
                            } else {
                                session.attributes["role"] = user.role
                                session.attributes["email"] = email
                                session.attributes["token"] = token

                                seeOther(URI("${properties.baseUri}/"))
                                        .cookie(ResponseCookie
                                                .from("XSRF-TOKEN", "${email}:${token}".encodeToBase64()!!)
                                                .maxAge(Duration.between(LocalDateTime.now(), user.tokenExpiration))
                                                .build())
                                        .build()
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
    private fun updateUserToken(user: User, locale: Locale, sendToken: Boolean = false): Mono<User> {
        val userToUpdate = user.generateNewToken()
        try {
            if (sendToken) {
                logger.info("A token ${userToUpdate.token} was sent by email")
                emailService.send("email-token", userToUpdate, locale)
            }
            return userRepository.save(userToUpdate)
        } catch (e: RuntimeException) {
            logger.error(e.message, e)
            return Mono.empty()
        }
    }

}

fun User.generateNewToken() = User(login, firstname, lastname, email, company, description, emailHash,
        photoUrl, role, links, legacyId, LocalDateTime.now().plusHours(48),
        UUID.randomUUID().toString().substring(0, 14).replace("-", ""))

fun User.updateEmail(cryptographer: Cryptographer, newEmail: String) = User(login, firstname, lastname, cryptographer.encrypt(newEmail), company, description, emailHash,
        photoUrl, role, links, legacyId, tokenExpiration, token)