package mixit.web.handler

import mixit.MixitProperties
import mixit.model.Language
import mixit.util.language
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.*
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*


@Component
class AuthenticationHandler(private val properties: MixitProperties) {

    fun loginView(req: ServerRequest) = ok().render("login")

    fun login(req: ServerRequest) = req.body(toFormData()).flatMap { data ->
        req.session().flatMap { session ->
            val formData = data.toSingleValueMap()
            if (formData["username"] == properties.admin.username && formData["password"] == properties.admin.password) {
                session.attributes["username"] =  data.toSingleValueMap()["username"]
                seeOther("${properties.baseUri}/admin")
            }
            else ok().render("login-error")
        }
    }

    fun logout(req: ServerRequest) = req.session().flatMap { session ->
            session.attributes.clear();
            mixit.util.permanentRedirect("${properties.baseUri}/")
        }
}
