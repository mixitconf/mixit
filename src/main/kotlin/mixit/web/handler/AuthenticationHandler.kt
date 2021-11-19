package mixit.web.handler

import mixit.MixitProperties
import mixit.model.User
import mixit.util.Cryptographer
import mixit.util.decodeFromBase64
import mixit.util.locale
import mixit.util.validator.EmailValidator
import mixit.web.handler.AuthenticationHandler.LoginError.DUPLICATE_EMAIL
import mixit.web.handler.AuthenticationHandler.LoginError.DUPLICATE_LOGIN
import mixit.web.handler.AuthenticationHandler.LoginError.INVALID_EMAIL
import mixit.web.handler.AuthenticationHandler.LoginError.REQUIRED_CREDENTIALS
import mixit.web.handler.AuthenticationHandler.LoginError.SIGN_UP_ERROR
import mixit.web.service.AuthenticationService
import mixit.web.service.CredentialValidatorException
import mixit.web.service.EmailValidatorException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.toFormData
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.seeOther
import org.springframework.web.reactive.function.server.ServerResponse.temporaryRedirect
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import java.net.URI
import java.net.URLDecoder

@Component
class AuthenticationHandler(
    private val authenticationService: AuthenticationService,
    private val properties: MixitProperties,
    private val emailValidator: EmailValidator,
    private val cryptographer: Cryptographer
) {

    private fun displayView(page: LoginPage, context: Map<String, String> = emptyMap()) = ok().render(page.template, context)
    private fun displayErrorView(error: LoginError): Mono<ServerResponse> = displayView(LoginPage.ERROR, mapOf(Pair("description", error.i18n)))

    private enum class LoginPage(val template: String) {
        // Page with a field to send his email
        LOGIN("login"),
        // An error page with a description of this error
        ERROR("login-error"),
        // Page with a field to send his token (received by email)
        CONFIRMATION("login-confirmation"),
        // Page displayed when a user is unknown
        CREATION("login-creation")
    }

    private enum class LoginError(val i18n: String) {
        // Email is invalid
        INVALID_EMAIL("login.error.creation.mail"),
        INVALID_TOKEN("login.error.badtoken.text"),
        TOKEN_SENT("login.error.sendtoken.text"),
        DUPLICATE_LOGIN("login.error.uniquelogin.text"),
        DUPLICATE_EMAIL("login.error.uniqueemail.text"),
        SIGN_UP_ERROR("login.error.field.text"),
        REQUIRED_CREDENTIALS("login.error.required.text")
    }

    private fun duplicateException(e: Throwable): LoginError = if (e.message != null && e.message!!.contains("login")) DUPLICATE_LOGIN else DUPLICATE_EMAIL

    /**
     * Display a view with a form to send the email of the user
     */
    fun loginView(req: ServerRequest) = displayView(LoginPage.LOGIN)

    /**
     * Action called by an HTTP post when a user send his email to connect to our application. If user is found
     * we send him a token by email. If not we ask him more informations to create an account. But before we
     * try to find him in ticket database
     */
    fun login(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap {
        try {
            val nonEncryptedMail = emailValidator.check(it.toSingleValueMap()["email"])
            authenticationService.searchUserByEmailOrCreateHimFromTicket(nonEncryptedMail)
                // If user is found we send him a token
                .flatMap { generateAndSendToken(req, it, nonEncryptedMail) }
                // An error can be thrown when we try to create a user from ticketting
                .onErrorResume { displayErrorView(duplicateException(it)) }
                // if user is not found we ask him if he wants to create a new account
                .switchIfEmpty(Mono.defer { displayView(LoginPage.CREATION, mapOf(Pair("email", nonEncryptedMail))) })
        } catch (e: EmailValidatorException) {
            displayErrorView(LoginError.INVALID_EMAIL)
        }
    }

    /**
     * Action called by an HTTP post when a user want to sign up to our application and create his account. If creation
     * is OK, we send him a token by email
     */
    fun signUp(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap {
        it.toSingleValueMap().let { formData ->
            try {
                val newUser = authenticationService.createUser(formData["email"], formData["firstname"], formData["lastname"])
                authenticationService.createUserIfEmailDoesNotExist(nonEncryptedMail = newUser.first, user = newUser.second)
                    .flatMap { generateAndSendToken(req, nonEncryptedMail = newUser.first, user = newUser.second) }
            } catch (e: EmailValidatorException) {
                displayErrorView(INVALID_EMAIL)
            } catch (e: CredentialValidatorException) {
                displayErrorView(SIGN_UP_ERROR)
            }
        }
    }

    private fun generateAndSendToken(req: ServerRequest, user: User, nonEncryptedMail: String): Mono<ServerResponse> =
        authenticationService.generateAndSendToken(user, req.locale())
            // if token is sent we call the the screen where user can type this token
            .flatMap { displayView(LoginPage.CONFIRMATION, mapOf(Pair("email", nonEncryptedMail))) }
            // An error can occur when email is sent
            .onErrorResume { displayErrorView(LoginError.TOKEN_SENT) }

    /**
     * Action when user wants to send his token to open a session. This token is valid only for a limited time
     * This action is launched when user clicks on the link sent by email
     */
    fun signInViaUrl(req: ServerRequest): Mono<ServerResponse> {
        val email = URLDecoder.decode(req.pathVariable("email"), "UTF-8").decodeFromBase64()
        val token = req.pathVariable("token")
        return displayView(
            LoginPage.CONFIRMATION,
            mapOf(
                Pair("email", emailValidator.check(email)),
                Pair("token", token)
            )
        )
    }

    /**
     * Action called by an HTTP post when user wants to send his email and his token to open a session.
     */
    fun signIn(req: ServerRequest): Mono<ServerResponse> = req.body(toFormData()).flatMap {
        it.toSingleValueMap().let { formData ->
            // If email or token are null we can't open a session
            if (formData["email"] == null || formData["token"] == null) {
                displayErrorView(REQUIRED_CREDENTIALS)
            } else {
                // Email sent can be crypted or not
                val nonEncryptedMail = if (formData["email"]!!.contains("@")) formData["email"] else cryptographer.decrypt(formData["email"])
                val token = formData["token"]!!

                authenticationService.checkUserEmailAndToken(nonEncryptedMail!!, token)
                    .flatMap { user ->
                        req.session().flatMap { session ->
                            session.apply {
                                attributes["role"] = user.role
                                attributes["email"] = nonEncryptedMail
                                attributes["token"] = token
                            }
                            seeOther(URI("${properties.baseUri}/me")).cookie(authenticationService.createCookie(user)).build()
                        }
                    }
                    .onErrorResume { displayErrorView(LoginError.INVALID_TOKEN) }
            }
        }
    }

    /**
     * Action when user wants to log out
     */
    fun logout(req: ServerRequest): Mono<ServerResponse> = req.session().flatMap { session ->
        Mono.justOrEmpty(session.getAttribute<String>("email"))
            .flatMap { authenticationService.clearToken(it).flatMap { clearSession(session) } }
            .switchIfEmpty(Mono.defer { clearSession(session) })
    }

    private fun clearSession(session: WebSession): Mono<ServerResponse> {
        session.attributes.apply {
            remove("email")
            remove("login")
            remove("token")
            remove("role")
        }
        return temporaryRedirect(URI("${properties.baseUri}/")).build()
    }
}
