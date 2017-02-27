package mixit.controller

import mixit.support.RouterFunctionProvider
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.*

@Controller
class AuthenticationController : RouterFunctionProvider() {

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: Routes = {
        accept(TEXT_HTML).route {
            GET("/login", this@AuthenticationController::loginView)
            // TODO Use POST
            GET("/logout", this@AuthenticationController::logout)
        }
        contentType(APPLICATION_FORM_URLENCODED).route {
            POST("/login", this@AuthenticationController::login)
        }
    }

    fun loginView(req: ServerRequest) = ok().render("login")

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

