package mixit.controller

import mixit.model.Article
import mixit.model.Language
import mixit.model.User
import mixit.repository.ArticleRepository
import mixit.support.MarkdownConverter
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.stereotype.Controller
import java.time.format.DateTimeFormatter


@Controller
class ArticleController(val repository: ArticleRepository, val markdownConverter: MarkdownConverter) : RouterFunction<ServerResponse> {

    private val frenchDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val englishDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/blog/") { findAllView(req) }
            GET("/blog/{id}") { findOneView(req) }
            // Old routes used by the previous version of our website
            GET("/articles/") { findAllView(req) }
            GET("/article/{id}") { findOneView(req) }
        }
        accept(APPLICATION_JSON).apply {
            GET("/api/blog/") { findAll() }
            GET("/api/blog/{id}") { findOne(req) }
        }
    }

    fun findOneView(req: ServerRequest) = repository.findOne(req.pathVariable("id")).then { a ->
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

    fun findAll() = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findAll()))


    private fun toArticleDto(article: Article, language: Language, markdownConverter: MarkdownConverter) = ArticleDto(
            article.id,
            article.author,
            if (language == Language.ENGLISH) article.addedAt.format(englishDateFormatter) else article.addedAt.format(frenchDateFormatter),
            article.title[language] ?: "",
            markdownConverter.toHTML(article.headline[language] ?: ""),
            markdownConverter.toHTML(if (article.content != null) article.content[language] else  ""))

    class ArticleDto(
        val id: String?,
        val author: User,
        val addedAt: String,
        val title: String,
        val headline: String,
        val content: String
    )
}
