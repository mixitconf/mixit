package mixit.security.handler

import mixit.MixitProperties
import mixit.security.MixitWebFilter.Companion.SESSION_EMAIL_KEY
import mixit.security.MixitWebFilter.Companion.SESSION_LOGIN_KEY
import mixit.security.MixitWebFilter.Companion.SESSION_ROLE_KEY
import mixit.security.MixitWebFilter.Companion.SESSION_TOKEN_KEY
import mixit.security.model.AuthenticationService
import mixit.security.model.Cryptographer
import mixit.user.model.User
import mixit.user.model.jsonToken
import mixit.util.decodeFromBase64
import mixit.util.errors.CredentialValidatorException
import mixit.util.errors.EmailValidatorException
import mixit.util.extractFormData
import mixit.util.locale
import mixit.util.validator.EmailValidator
import org.springframework.stereotype.Component
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

    companion object {
        const val CONTEXT = "isLogin"
    }

    private fun displayView(page: LoginPage, context: Context, params: Map<String, String> = emptyMap()) =
        ok().render(page.template, params + mapOf(Pair(CONTEXT, context == Context.Login)))

    private fun displayErrorView(error: LoginError, context: Context): Mono<ServerResponse> =
        displayView(LoginPage.ERROR, context, mapOf(Pair("description", error.i18n)))

    private enum class Context { Login, Newsletter }

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

    private fun duplicateException(e: Throwable): LoginError =
        if (e.message != null && e.message!!.contains("login")) LoginError.DUPLICATE_LOGIN else LoginError.DUPLICATE_EMAIL

    /**
     * Display a view with a form to send the email of the user
     */
    fun loginView(req: ServerRequest) =
        displayView(LoginPage.LOGIN, Context.Login)

    /**
     * Display a view with a form to let user subscribe to our newsletter
     */
    fun newsletterView(req: ServerRequest) =
        displayView(LoginPage.LOGIN, Context.Newsletter)

    /**
     * Action called by an HTTP post when a user send his email to connect to our application. If user is found
     * we send him a token by email. If not we ask him more informations to create an account. But before we
     * try to find him in ticket database
     */
    fun login(req: ServerRequest): Mono<ServerResponse> =
        doLogin(req, Context.Login)

    /**
     * Action called by an HTTP post when a user send his email to subscribe to our newsletter. If user is found
     * we send him a token by email. If not we ask him more informations to create an account. But before we
     * try to find him in ticket database
     */
    fun sendEmailForNewsletter(req: ServerRequest): Mono<ServerResponse> =
        doLogin(req, Context.Newsletter)

    private fun doLogin(req: ServerRequest, context: Context): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            try {
                val nonEncryptedMail = emailValidator.check(formData["email"])
                authenticationService.searchUserByEmailOrCreateHimFromTicket(nonEncryptedMail)
                    // If user is found we send him a token
                    .flatMap { generateAndSendToken(req, it, nonEncryptedMail, context) }
                    // An error can be thrown when we try to create a user from ticketting
                    .onErrorResume { displayErrorView(duplicateException(it), context) }
                    // if user is not found we ask him if he wants to create a new account
                    .switchIfEmpty(
                        Mono.defer {
                            displayView(
                                LoginPage.CREATION,
                                context,
                                mapOf(Pair("email", nonEncryptedMail))
                            )
                        }
                    )
            } catch (e: EmailValidatorException) {
                displayErrorView(LoginError.INVALID_EMAIL, context)
            }
        }

    /**
     * Action called by an HTTP post when a user want to send its token received by email
     */
    fun sendToken(req: ServerRequest): Mono<ServerResponse> =
        doSendToken(req, Context.Login)

    /**
     * Action called by an HTTP post when a user want to send its token received by email
     */
    fun sendTokenForNewsletter(req: ServerRequest): Mono<ServerResponse> =
        doSendToken(req, Context.Newsletter)

    private fun doSendToken(req: ServerRequest, context: Context): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            try {
                val nonEncryptedMail = emailValidator.check(formData["email"])
                authenticationService.searchUserByEmailOrCreateHimFromTicket(nonEncryptedMail)
                    // If user is found we send him a token
                    .flatMap { displayView(LoginPage.CONFIRMATION, context, mapOf(Pair("email", nonEncryptedMail))) }
                    // An error can be thrown when we try to create a user from ticketting
                    .onErrorResume { displayErrorView(duplicateException(it), context) }
                    // if user is not found we ask him if he wants to create a new account
                    .switchIfEmpty(
                        Mono.defer {
                            displayView(
                                LoginPage.CREATION,
                                context,
                                mapOf(Pair("email", nonEncryptedMail))
                            )
                        }
                    )
            } catch (e: EmailValidatorException) {
                displayErrorView(LoginError.INVALID_EMAIL, context)
            }
        }

    /**
     * Action called by an HTTP post when a user want to sign up to our application and create his account. If creation
     * is OK, we send him a token by email
     */
    fun signUp(req: ServerRequest): Mono<ServerResponse> =
        doSignUp(req, Context.Login)

    /**
     * Action called by an HTTP post when a user want to subscribe to our newsletter and create his account. If creation
     * is OK, we send him a token by email
     */
    fun signUpForNewsletter(req: ServerRequest): Mono<ServerResponse> =
        doSignUp(req, Context.Newsletter)

    private fun doSignUp(req: ServerRequest, context: Context): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            try {
                val newUser =
                    authenticationService.createUser(formData["email"], formData["firstname"], formData["lastname"])

                authenticationService.createUserIfEmailDoesNotExist(
                    nonEncryptedMail = newUser.first,
                    user = newUser.second
                )
                    .flatMap {
                        generateAndSendToken(
                            req,
                            nonEncryptedMail = newUser.first,
                            user = newUser.second,
                            context = context
                        )
                    }
            } catch (e: EmailValidatorException) {
                displayErrorView(LoginError.INVALID_EMAIL, context)
            } catch (e: CredentialValidatorException) {
                displayErrorView(LoginError.SIGN_UP_ERROR, context)
            }
        }

    private fun generateAndSendToken(
        req: ServerRequest,
        user: User,
        nonEncryptedMail: String,
        context: Context
    ): Mono<ServerResponse> =
        authenticationService.generateAndSendToken(
            user,
            req.locale(),
            nonEncryptedMail,
            tokenForNewsletter = context == Context.Newsletter
        )
            // if token is sent we call the the screen where user can type this token
            .flatMap { displayView(LoginPage.CONFIRMATION, context, mapOf(Pair("email", nonEncryptedMail))) }
            // An error can occur when email is sent
            .onErrorResume { displayErrorView(LoginError.TOKEN_SENT, context) }

    /**
     * Action when user wants to send his token to open a session. This token is valid only for a limited time
     * This action is launched when user clicks on the link sent by email
     */
    fun signInViaUrl(req: ServerRequest): Mono<ServerResponse> =
        doSignInViaUrl(req, Context.Login)

    /**
     * Action when user wants to send his token to open a session. This token is valid only for a limited time
     * This action is launched when user clicks on the link sent by email
     */
    fun signInViaUrlForNewsletter(req: ServerRequest): Mono<ServerResponse> =
        doSignInViaUrl(req, Context.Newsletter)

    private fun doSignInViaUrl(req: ServerRequest, context: Context): Mono<ServerResponse> {
        val email = URLDecoder.decode(req.pathVariable("email"), "UTF-8").decodeFromBase64()
        val token = req.pathVariable("token")
        return displayView(
            LoginPage.CONFIRMATION,
            context,
            mapOf(
                Pair("email", emailValidator.check(email)),
                Pair("token", token)
            )
        )
    }

    /**
     * Action called by an HTTP post when user wants to send his email and his token to open a session.
     */
    fun signIn(req: ServerRequest) =
        doSignIn(req, Context.Login)

    /**
     * Action called by an HTTP post when user wants to subscribe our newskletter
     */
    fun subscribeNewsletter(req: ServerRequest) =
        doSignIn(req, Context.Newsletter)

    private fun doSignIn(req: ServerRequest, context: Context): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // If email or token are null we can't open a session
            if (formData["email"] == null || formData["token"] == null) {
                displayErrorView(LoginError.REQUIRED_CREDENTIALS, context)
            } else {
                // Email sent can be crypted or not
                val nonEncryptedMail =
                    if (formData["email"]!!.contains("@")) formData["email"] else cryptographer.decrypt(formData["email"])
                val token = formData["token"]!!

                authenticationService.checkUserEmailAndToken(nonEncryptedMail!!, token)
                    .flatMap { authenticationService.updateNewsletterSubscription(it, context == Context.Newsletter) }
                    .flatMap { user ->
                        req.session().flatMap { session ->
                            val test = user.jsonToken(cryptographer)
                            val cookie = authenticationService.createCookie(user)
                            session.apply {
                                attributes[SESSION_ROLE_KEY] = user.role
                                attributes[SESSION_EMAIL_KEY] = nonEncryptedMail
                                attributes[SESSION_TOKEN_KEY] = token
                                attributes[SESSION_LOGIN_KEY] = user.login
                            }
                            seeOther(URI("${properties.baseUri}/me")).cookie(cookie).build()
                        }
                    }
                    .onErrorResume { displayErrorView(LoginError.INVALID_TOKEN, context) }
            }
        }


    /**
     * Action when user wants to log out
     */
    fun logout(req: ServerRequest): Mono<ServerResponse> =
        req.session().flatMap { session ->
            Mono.justOrEmpty(session.getAttribute<String>("email"))
                .flatMap { authenticationService.clearToken(it).flatMap { clearSession(session) } }
                .switchIfEmpty(Mono.defer { clearSession(session) })
        }

    private fun clearSession(session: WebSession): Mono<ServerResponse> {
        session.attributes.apply {
            remove(SESSION_EMAIL_KEY)
            remove(SESSION_LOGIN_KEY)
            remove(SESSION_TOKEN_KEY)
            remove(SESSION_ROLE_KEY)
        }
        return temporaryRedirect(URI("${properties.baseUri}/")).build()
    }
}
