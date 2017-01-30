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
                    userRepository
                            .save(User(
                                    user.login,
                                    user.firstname,
                                    user.lastname,
                                    user.email,
                                    data.toSingleValueMap()["provider"],
                                    company = user.company,
                                    description = user.description,
                                    logoUrl = user.logoUrl,
                                    role = user.role,
                                    links = user.links,
                                    legacyId = user.legacyId
                            ))
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
        ServerResponse.status(HttpStatus.SEE_OTHER).location(oauthService.providerOauthUri(req)).build()
    }

//    fun login(req: ServerRequest) = req.body(toFormData()).then { data ->
//        val provider: String? = data.toSingleValueMap()["provider"]
//
//        if (provider.isNullOrEmpty()) {
//            req.session().then { session ->
//                val formData = data.toSingleValueMap()
//                if (formData["username"] == properties.admin.username && formData["password"] == properties.admin.password) {
//                    session.attributes["username"] = data.toSingleValueMap()["username"]
//                    seeOther("${properties.baseUri}/admin")
//                } else ok().render("login-error")
//            }
//        } else {
//            // When user chooses an OAuth authentication we call the provider to start the dance
//            val oauthService: OAuth = oAuthFactory.create(provider.orEmpty())
//            ServerResponse.status(HttpStatus.SEE_OTHER).location(oauthService.providerOauthUri(req)).build()
//        }
//    }


    fun logout(req: ServerRequest) = req.session().then { session ->
        session.attributes.remove("username")
        ok().render("home")
    }

    /**
     * Endpoint called by an OAuth provider when the user is authenticated
     */
    fun oauthCallback(req: ServerRequest) = req.session().then { session ->
        val oauthService: OAuth = oAuthFactory.create(req.pathVariable("provider"))
        val oauthId = oauthService.getOAuthId(req)

        if (oauthId.isPresent()) {
            userRepository.findByLogin(oauthId.get()).then { user ->
                if (user != null) {
                    session.attributes["username"] = "${user.firstname}"
                    ok().render("home")
                } else {
                    userRepository.save(User(oauthId.get(), "", "", "")).block()
                    ok().render("login-new")
                }
            }
        } else ok().render("login-error")
    }


}
