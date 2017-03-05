package mixit.controller

import mixit.model.Post
import mixit.model.Language
import mixit.model.Language.*
import mixit.model.User
import mixit.repository.PostRepository
import mixit.support.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.*
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.ServerResponse.*

@Controller
class BlogController(val repository: PostRepository,
                     val markdownConverter: MarkdownConverter,
                     @Value("\${baseUri}") val baseUri: String) : RouterFunctionProvider() {

    // TODO Remove this@BlogController when KT-15667 will be fixed
    override val routes: Routes = {
        ("/blog" and accept(TEXT_HTML)).route {
            GET("/", this@BlogController::findAllView)
            GET("/{slug}", this@BlogController::findOneView)
        }
        ("/api/blog" and accept(APPLICATION_JSON)).route {
            GET("/", this@BlogController::findAll)
            GET("/{id}", this@BlogController::findOne)
        }
    }

    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug"), req.language()).then { a ->
        val model = mapOf(Pair("post", a.toDto(req.language(), markdownConverter)))
        ok().render("post", model)
    }.otherwiseIfEmpty(repository.findBySlug(req.pathVariable("slug"), if (req.language() == FRENCH) ENGLISH else FRENCH).then { a ->
        permanentRedirect("$baseUri${if (req.language() == ENGLISH) "/en" else ""}/blog/${a.slug[req.language()]}")
    })

    fun findAllView(req: ServerRequest) = repository.findAll(req.language()).collectList().then { articles ->
        val model = mapOf(Pair("posts", articles.map { it.toDto(req.language(), markdownConverter) }))
        ok().render("blog", model)
    }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("id")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())


    private fun Post.toDto(language: Language, markdownConverter: MarkdownConverter) = PostDto(
            id,
            slug[language] ?: "",
            author,
            addedAt.format(language),
            title[language] ?: "",
            markdownConverter.toHTML(headline[language] ?: ""),
            markdownConverter.toHTML(if (content != null) content[language] else  ""))

    class PostDto(
        val id: String?,
        val slug: String,
        val author: User,
        val addedAt: String,
        val title: String,
        val headline: String,
        val content: String
    )
}
