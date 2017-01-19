package mixit.controller

import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.*

@Controller
class AuthenticationController : RouterFunction<ServerResponse> {

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/login") { loginView() }
            // TODO Use POST
            GET("/logout") { logout(req) }
        }
        accept(MediaType.APPLICATION_FORM_URLENCODED).apply {
            POST("/login") { login(req) }
        }
    }

    fun loginView() = ok().render("login")

    fun login(req: ServerRequest) = req.body(BodyExtractors.toFormData()).then { data ->
        req.session().then { session ->
            session.attributes["username"] =  data.toSingleValueMap()["username"]
            ok().render("index")
        }
    }

    fun logout(req: ServerRequest) = req.session().then { session ->
        session.attributes.remove("username")
        ok().render("index")
    }
}

