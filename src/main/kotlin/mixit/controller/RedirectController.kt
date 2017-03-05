package mixit.controller

import mixit.repository.PostRepository
import mixit.repository.TalkRepository
import mixit.util.RouterFunctionProvider
import mixit.util.language
import mixit.util.permanentRedirect
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.Routes
import org.springframework.web.reactive.function.server.ServerRequest

@Controller
class RedirectController(val postRepository: PostRepository,
                         val talkRepository: TalkRepository,
                         @Value("\${baseUri}") val baseUri: String) : RouterFunctionProvider() {

    override val routes: Routes = {
        accept(TEXT_HTML).route {
            "/articles".route {
                GET("/") { permanentRedirect("$baseUri/blog") }
                (GET("/{id}") or GET("/{id}/")) { redirectOneArticleView(it) }
            }
            GET("/article/{id}/", this@RedirectController::redirectOneArticleView)

            GET("/2017/") { permanentRedirect("$baseUri/2017") }
            GET("/2016/") { permanentRedirect("$baseUri/2016") }
            GET("/2015/") { permanentRedirect("$baseUri/2015") }
            GET("/2014/") { permanentRedirect("$baseUri/2014") }
            GET("/2013/") { permanentRedirect("$baseUri/2013") }
            GET("/2012/") { permanentRedirect("$baseUri/2012") }
            (GET("/session/{id}") or GET("/session/{id}/") or GET("/session/{id}/{sluggifiedTitle}/") or GET("/session/{id}/{sluggifiedTitle}")) { redirectOneSessionView(it) }

            (GET("/member/{login}") or GET("/profile/{login}") or GET("/member/sponsor/{login}") or GET("/member/member/{login}")) { permanentRedirect("$baseUri/user/${it.pathVariable("login")}") }
            GET("/sponsors/") { permanentRedirect("$baseUri/sponsors") }

            GET("/about/") { permanentRedirect("$baseUri/about") }

        }
    }

    fun redirectOneArticleView(req: ServerRequest) = postRepository.findOne(req.pathVariable("id")).then { a ->
        permanentRedirect("$baseUri/blog/${a.slug[req.language()]}")
    }

    fun redirectOneSessionView(req: ServerRequest) = talkRepository.findOne(req.pathVariable("id")).then { s ->
        permanentRedirect("$baseUri/talk/${s.slug}")
    }

}

