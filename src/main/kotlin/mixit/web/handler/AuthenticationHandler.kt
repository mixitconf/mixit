package mixit.web.handler

import mixit.MixitProperties
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.*
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*


@Component
class AuthenticationHandler(val mixitProperties: MixitProperties) {

    fun loginView(req: ServerRequest) = ok().render("login")

    fun login(req: ServerRequest) = req.body(toFormData()).then { data ->
        req.session().then { session ->
            val formData = data.toSingleValueMap()
            if (formData["username"] == mixitProperties.admin.username && formData["password"] == mixitProperties.admin.password) {
                session.attributes["username"] =  data.toSingleValueMap()["username"]
                seeOther("${mixitProperties.baseUri}/admin")
            }
            else ok().render("login-error")
        }
    }

    fun logout(req: ServerRequest) = req.session().then { session ->
        session.attributes.remove("username")
        ok().render("index")
    }
}
