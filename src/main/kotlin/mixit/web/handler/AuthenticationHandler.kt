package mixit.web.handler

import mixit.MixitProperties
import mixit.model.User
import mixit.repository.UserRepository
import mixit.support.security.OAuth
import mixit.support.security.OAuthFactory
import mixit.support.security.OAuthProvider
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.toFormData
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.util.*


@Component
class AuthenticationHandler(val properties: MixitProperties, val userRepository: UserRepository, val oAuthFactory: OAuthFactory) {

    fun loginView(req: ServerRequest) = ok().render("login")

    fun login(req: ServerRequest) = req.body(toFormData()).then { data ->
        val formData = data.toSingleValueMap()
        userRepository
                .findByEmail(formData["email"].orEmpty())
                .then { user ->
                    if (user.oAuthProvider == null)
                        ok().render("login-provider", mapOf(Pair("user", user)))
                    else {
                        callOauthProvider(req, user)
                    }
                }
                .defaultIfEmpty(
                        ok().render("login-create", mapOf(Pair("email", formData["email"].orEmpty()))).block()
                )
    }

    fun associateProvider(req: ServerRequest) = req.body(toFormData()).then { data ->
        val formData = data.toSingleValueMap()
        userRepository
                .findByEmail(formData["email"].orEmpty())
                .then { user ->
                    updateUser(user, user.login, data.toSingleValueMap()["provider"].orEmpty())
                            .then(callOauthProvider(req, user))
                }
    }

    fun createLoginWithProvider(req: ServerRequest) = req.body(toFormData()).then { data ->
        val formData = data.toSingleValueMap()
        val user = User(
                UUID.randomUUID().toString(),
                formData["firstname"].orEmpty(),
                formData["lastname"].orEmpty(),
                formData["email"].orEmpty(),
                oAuthProvider = formData["provider"].orEmpty());

        userRepository
                .save(user)
                .then(callOauthProvider(req, user))
    }

    fun callOauthProvider(req: ServerRequest, user: User) = req.session().then { session ->
        // When user chooses an OAuth authentication we call the provider to start the dance
        session.attributes["email"] = user.email
        val oauthService: OAuth = oAuthFactory.create(user.oAuthProvider!!)
        ServerResponse.status(HttpStatus.SEE_OTHER).location(oauthService.providerOauthUri(req, user)).build()
    }

    fun logout(req: ServerRequest) = req.session().then { session ->
        session.attributes.remove("email")
        ok().render("home")
    }

    /**
     * Endpoint called by an OAuth provider when the user is authenticated
     */
    fun oauthCallback(req: ServerRequest) = req.session().then { session ->
        val provider = req.pathVariable("provider")
        val oauthService: OAuth = oAuthFactory.create(provider)
        val email = session.getAttribute<String>("email")

        userRepository
                .findByEmail(email.get())
                .then { user ->
                    val oauthId = oauthService.getOAuthId(req, user)

                    if (oauthId.isPresent()) {
                        updateUser(user, oauthId.get(), provider).then { user ->
                            ok().render("home")
                        }
                    } else ok().render("login-error")
                }
    }


    //TODO to refactor
    fun updateUser(user: User, login: String, provider: String) = userRepository
            .save(User(
                    login,
                    user.firstname,
                    user.lastname,
                    user.email,
                    provider,
                    company = user.company,
                    description = user.description,
                    logoUrl = user.logoUrl,
                    role = user.role,
                    links = user.links,
                    legacyId = user.legacyId
            ))

}
