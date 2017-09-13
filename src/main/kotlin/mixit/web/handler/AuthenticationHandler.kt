package mixit.web.handler

import mixit.MixitProperties
import mixit.model.Language
import mixit.util.language
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.*
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

const val USERNAME_FIELD = "username"
const val USERNAME_PASSWORD = "password"

@Component
class AuthenticationHandler(private val properties: MixitProperties) {


    fun loginView(req: ServerRequest) = ok().render("login")

    fun login(req: ServerRequest) = req.body(toFormData()).flatMap { data ->
        req.session().flatMap { session ->
            val formData = data.toSingleValueMap()
            if (formData[USERNAME_FIELD] == properties.admin.username && formData[USERNAME_PASSWORD] == properties.admin.password) {
                session.attributes[USERNAME_FIELD] = data.toSingleValueMap()[USERNAME_FIELD]
                if(session.isStarted){
                    session.maxIdleTime = Duration.ofHours(12)
                }
                seeOther("${properties.baseUri}/admin")
            } else ok().render("login-error")
        }
    }

    fun logout(req: ServerRequest): Mono<ServerResponse> = req.session().flatMap { session ->
        session.attributes.remove(USERNAME_FIELD)
        temporaryRedirect(URI("${properties.baseUri}/")).build()
    }
}
