package mixit.controller

import mixit.model.Article
import mixit.model.Language
import mixit.model.User
import mixit.repository.ArticleRepository
import mixit.support.LazyRouterFunction
import mixit.support.MarkdownConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.stereotype.Controller
import org.springframework.web.reactive.function.server.ServerResponse.status
import java.net.URI.*
import java.time.format.DateTimeFormatter


@Controller
class ArticleController(val repository: ArticleRepository, val markdownConverter: MarkdownConverter,
                        @Value("\${baseUri}") val baseUri: String) : LazyRouterFunction() {

    private val frenchDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val englishDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: Routes.() -> Unit = {
        accept(TEXT_HTML).route {
            GET("/blog/", this@ArticleController::findAllView)
            GET("/blog/{slug}", this@ArticleController::findOneView)
            // /articles/** are old routes used by the previous version of our website
            GET("/articles/") { status(PERMANENT_REDIRECT).location(create("$baseUri/blog/")).build() }
            (GET("/article/{id}") or GET("/article/{id}/")) { status(PERMANENT_REDIRECT).location(create("$baseUri/blog/${it.pathVariable("id")}")).build() }
        }
        accept(APPLICATION_JSON).route {
            GET("/api/blog/", this@ArticleController::findAll)
            GET("/api/blog/{id}", this@ArticleController::findOne)
        }
    }

    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug")).then { a ->
        val languageTag = req.headers().header(HttpHeaders.ACCEPT_LANGUAGE).first()
        val language = Language.findByTag(languageTag)
        val model = mapOf(Pair("article", toArticleDto(a, language, markdownConverter)))
        ok().render("article", model)
    }

    fun findAllView(req: ServerRequest) = repository.findAll().collectList().then { articles ->
        val languageTag = req.headers().header(HttpHeaders.ACCEPT_LANGUAGE).first()
        val language = Language.findByTag(languageTag)
        ok().render("articles",  mapOf(Pair("articles", articles.map { toArticleDto(it, language, markdownConverter) })))
    }

    fun findOne(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findOne(req.pathVariable("id"))))

    fun findAll(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findAll()))


    private fun toArticleDto(article: Article, language: Language, markdownConverter: MarkdownConverter) = ArticleDto(
            article.id,
            article.slug[language] ?: "",
            article.author,
            if (language == Language.ENGLISH) article.addedAt.format(englishDateFormatter) else article.addedAt.format(frenchDateFormatter),
            article.title[language] ?: "",
            markdownConverter.toHTML(article.headline[language] ?: ""),
            markdownConverter.toHTML(if (article.content != null) article.content[language] else  ""))

    class ArticleDto(
        val id: String?,
        val slug: String,
        val author: User,
        val addedAt: String,
        val title: String,
        val headline: String,
        val content: String
    )
}
