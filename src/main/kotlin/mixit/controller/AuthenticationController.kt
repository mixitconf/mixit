package mixit.controller

import mixit.util.RouterFunctionProvider
import mixit.util.found
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.BodyExtractors.*
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok


@Controller
class AuthenticationController(@Value("\${admin.username}") val username: String,
                               @Value("\${admin.password}") val password: String,
                               @Value("\${baseUri}") val baseUri: String) : RouterFunctionProvider() {

    override val routes: Routes = {
        accept(TEXT_HTML).route {
            GET("/login") { ok().render("login") }
            // TODO Use POST
            GET("/logout", this@AuthenticationController::logout)
        }
        contentType(APPLICATION_FORM_URLENCODED).route {
            POST("/login", this@AuthenticationController::login)
        }
    }

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
