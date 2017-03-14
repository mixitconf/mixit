package mixit.web.handler

import mixit.util.found
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors.*
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*


@Component
class AuthenticationHandler(@Value("\${admin.username}") val username: String,
                            @Value("\${admin.password}") val password: String,
                            @Value("\${baseUri}") val baseUri: String) {

    fun loginView(req: ServerRequest) = ok().render("login")

    fun login(req: ServerRequest) = req.body(toFormData()).then { data ->
        req.session().then { session ->
            val formData = data.toSingleValueMap()
            if (formData["username"] == username && formData["password"] == password) {
                session.attributes["username"] =  data.toSingleValueMap()["username"]
                found("$baseUri/admin")
            }
            else ok().render("login-error")
        }
    }

    fun logout(req: ServerRequest) = req.session().then { session ->
        session.attributes.remove("username")
        ok().render("index")
    }
}
