package mixit.security.handler

import mixit.MixitProperties
import mixit.routes.MustacheI18n.DESCRIPTION
import mixit.routes.MustacheI18n.EMAIL
import mixit.routes.MustacheI18n.TOCKEN
import mixit.routes.MustacheTemplate
import mixit.routes.MustacheTemplate.Login
import mixit.routes.MustacheTemplate.LoginConfirmation
import mixit.routes.MustacheTemplate.LoginCreation
import mixit.security.MixitWebFilter.Companion.SESSION_EMAIL_KEY
import mixit.security.MixitWebFilter.Companion.SESSION_LOGIN_KEY
import mixit.security.MixitWebFilter.Companion.SESSION_ROLE_KEY
import mixit.security.MixitWebFilter.Companion.SESSION_TOKEN_KEY
import mixit.security.handler.LoginError.DUPLICATE_EMAIL
import mixit.security.handler.LoginError.DUPLICATE_LOGIN
import mixit.security.model.AuthenticationService
import mixit.security.model.Cryptographer
import mixit.user.model.User
import mixit.user.model.jsonToken
import mixit.util.decode
import mixit.util.errors.CredentialValidatorException
import mixit.util.errors.DuplicateException
import mixit.util.errors.EmailSenderException
import mixit.util.errors.EmailValidatorException
import mixit.util.extractFormData
import mixit.util.locale
import mixit.util.seeOther
import mixit.util.temporaryRedirect
import mixit.util.validator.EmailValidator
import mixit.util.webSession
import mixit.util.webSessionOrNull
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
import org.springframework.web.server.WebSession

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

    private enum class Context { Login, Newsletter }

    private suspend fun displayView(
        template: MustacheTemplate,
        context: Context,
        params: Map<String, String> = emptyMap()
    ): ServerResponse =
        ok().renderAndAwait(template.template, params + mapOf(Pair(CONTEXT, context == Context.Login)))

    private suspend fun displayErrorView(error: LoginError, context: Context): ServerResponse =
        displayView(MustacheTemplate.LoginError, context, mapOf(DESCRIPTION to error.i18n))

    private fun duplicateException(e: Throwable): LoginError =
        if (e.message != null && e.message!!.contains("login")) DUPLICATE_LOGIN else DUPLICATE_EMAIL

    /**
     * Display a view with a form to send the email of the user
     */
    suspend fun loginView(req: ServerRequest): ServerResponse =
        displayView(Login, Context.Login)

    /**
     * Display a view with a form to let user subscribe to our newsletter
     */
    suspend fun newsletterView(req: ServerRequest): ServerResponse {
        val session = req.webSessionOrNull() ?: return displayView(Login, Context.Newsletter)
        with(session) {
            if (attributes[SESSION_EMAIL_KEY] != null &&
                attributes[SESSION_TOKEN_KEY] != null &&
                attributes[SESSION_LOGIN_KEY] != null
            ) {
                return doSignIn(
                    req,
                    Context.Newsletter,
                    attributes[SESSION_EMAIL_KEY]?.toString(),
                    attributes[SESSION_TOKEN_KEY]?.toString()
                )
            }
        }
        return displayView(Login, Context.Newsletter)
    }

    /**
     * Action called by an HTTP post when a user send his email to connect to our application. If user is found
     * we send him a token by email. If not we ask him more informations to create an account. But before we
     * try to find him in ticket database
     */
    suspend fun login(req: ServerRequest): ServerResponse =
        doLogin(req, Context.Login)

    /**
     * Action called by an HTTP post when a user send his email to subscribe to our newsletter. If user is found
     * we send him a token by email. If not we ask him more informations to create an account. But before we
     * try to find him in ticket database
     */
    suspend fun sendEmailForNewsletter(req: ServerRequest): ServerResponse =
        doLogin(req, Context.Newsletter)

    private suspend fun doLogin(req: ServerRequest, context: Context): ServerResponse {
        val formData = req.extractFormData()
        return try {
            val nonEncryptedMail = emailValidator.check(formData[EMAIL])
            val user = authenticationService.searchUserByEmailOrCreateHimFromTicket(nonEncryptedMail)
                ?: return displayView(LoginCreation, context, mapOf(EMAIL to nonEncryptedMail))

            // If user is found we send him a token
            generateAndSendToken(req, user, nonEncryptedMail, context)
        } catch (e: DuplicateException) {
            // An error can be thrown when we try to create a user from ticketting
            displayErrorView(duplicateException(e), context)
        } catch (e: EmailValidatorException) {
            displayErrorView(LoginError.INVALID_EMAIL, context)
        }
    }

    /**
     * Action called by an HTTP post when a user want to send its token received by email
     */
    suspend fun sendToken(req: ServerRequest): ServerResponse =
        doSendToken(req, Context.Login)

    /**
     * Action called by an HTTP post when a user want to send its token received by email
     */
    suspend fun sendTokenForNewsletter(req: ServerRequest): ServerResponse =
        doSendToken(req, Context.Newsletter)

    private suspend fun doSendToken(req: ServerRequest, context: Context): ServerResponse {
        val formData = req.extractFormData()
        return try {
            val nonEncryptedMail = emailValidator.check(formData[EMAIL])
            authenticationService.searchUserByEmailOrCreateHimFromTicket(nonEncryptedMail) ?: return displayView(
                LoginCreation,
                context,
                mapOf(EMAIL to nonEncryptedMail)
            )
            // If user is found we send him a token
            displayView(
                LoginConfirmation,
                context,
                mapOf(EMAIL to nonEncryptedMail)
            )
        } catch (e: DuplicateException) {
            displayErrorView(duplicateException(e), context)
        } catch (e: EmailValidatorException) {
            displayErrorView(LoginError.INVALID_EMAIL, context)
        }
    }

    /**
     * Action called by an HTTP post when a user want to sign up to our application and create his account. If creation
     * is OK, we send him a token by email
     */
    suspend fun signUp(req: ServerRequest): ServerResponse =
        doSignUp(req, Context.Login)

    /**
     * Action called by an HTTP post when a user want to subscribe to our newsletter and create his account. If creation
     * is OK, we send him a token by email
     */
    suspend fun signUpForNewsletter(req: ServerRequest): ServerResponse =
        doSignUp(req, Context.Newsletter)

    private suspend fun doSignUp(req: ServerRequest, context: Context): ServerResponse {
        val formData = req.extractFormData()
        return try {
            val (email, newUser) = authenticationService.createUser(
                formData[EMAIL],
                formData["firstname"],
                formData["lastname"]
            )

            val persistedUser = authenticationService.createUserIfEmailDoesNotExist(
                nonEncryptedMail = email,
                user = newUser
            )
            generateAndSendToken(
                req,
                nonEncryptedMail = email,
                user = persistedUser,
                context = context
            )
        } catch (e: EmailValidatorException) {
            displayErrorView(LoginError.INVALID_EMAIL, context)
        } catch (e: CredentialValidatorException) {
            displayErrorView(LoginError.SIGN_UP_ERROR, context)
        }
    }

    private suspend fun generateAndSendToken(
        req: ServerRequest,
        user: User,
        nonEncryptedMail: String,
        context: Context
    ): ServerResponse =
        try {
            authenticationService.generateAndSendToken(
                user,
                req.locale(),
                nonEncryptedMail,
                tokenForNewsletter = (context == Context.Newsletter)
            )
            displayView(LoginConfirmation, context, mapOf(EMAIL to nonEncryptedMail))
        } catch (e: EmailSenderException) {
            displayErrorView(LoginError.TOKEN_SENT, context)
        }

    /**
     * Action when user wants to send his token to open a session. This token is valid only for a limited time
     * This action is launched when user clicks on the link sent by email
     */
    suspend fun signInViaUrl(req: ServerRequest): ServerResponse =
        doSignInViaUrl(req, Context.Login)

    /**
     * Action when user wants to send his token to open a session. This token is valid only for a limited time
     * This action is launched when user clicks on the link sent by email
     */
    suspend fun signInViaUrlForNewsletter(req: ServerRequest): ServerResponse =
        doSignInViaUrl(req, Context.Newsletter)

    private suspend fun doSignInViaUrl(req: ServerRequest, context: Context): ServerResponse {
        val email = req.decode(EMAIL)
        val token = req.pathVariable(TOCKEN)
        return displayView(
            LoginConfirmation,
            context,
            mapOf(EMAIL to emailValidator.check(email), TOCKEN to token)
        )
    }

    /**
     * Action called by an HTTP post when user wants to send his email and his token to open a session.
     */
    suspend fun signIn(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        return doSignIn(req, Context.Login, formData[EMAIL], formData[TOCKEN])
    }

    /**
     * Action called by an HTTP post when user wants to subscribe our newskletter
     */
    suspend fun subscribeNewsletter(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        return doSignIn(req, Context.Newsletter, formData[EMAIL], formData[TOCKEN])
    }

    private suspend fun doSignIn(
        req: ServerRequest,
        context: Context,
        email: String?,
        token: String?
    ): ServerResponse =
        // If email or token are null we can't open a session
        if (email == null || token == null) {
            displayErrorView(LoginError.REQUIRED_CREDENTIALS, context)
        } else {
            // Email sent can be crypted or not
            val nonEncryptedMail = if (email.contains("@")) email else cryptographer.decrypt(email)

            val user = authenticationService.checkUserEmailAndToken(nonEncryptedMail!!, token)
                .let { authenticationService.updateNewsletterSubscription(it, context == Context.Newsletter) }

            val session = req.webSession()
            val test = user.jsonToken(cryptographer)
            val cookie = authenticationService.createCookie(user)
            session.apply {
                attributes[SESSION_ROLE_KEY] = user.role
                attributes[SESSION_EMAIL_KEY] = nonEncryptedMail
                attributes[SESSION_TOKEN_KEY] = token
                attributes[SESSION_LOGIN_KEY] = user.login
            }
            seeOther("${properties.baseUri}/me", cookie)
        }

    /**
     * Action when user wants to log out
     */
    suspend fun logout(req: ServerRequest): ServerResponse =
        req.webSessionOrNull()
            ?.let { session ->
                session.getAttribute<String>(EMAIL)?.also {
                    authenticationService.clearToken(it)
                }
                clearSession(session)
            }
            ?: temporaryRedirect("${properties.baseUri}/")

    private suspend fun clearSession(session: WebSession): ServerResponse {
        session.attributes.apply {
            remove(SESSION_EMAIL_KEY)
            remove(SESSION_LOGIN_KEY)
            remove(SESSION_TOKEN_KEY)
            remove(SESSION_ROLE_KEY)
        }
        return temporaryRedirect("${properties.baseUri}/")
    }
}
