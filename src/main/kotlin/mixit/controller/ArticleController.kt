package mixit.controller

import mixit.model.Article
import mixit.model.Language
import mixit.model.User
import mixit.repository.ArticleRepository
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.parser.Parser
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.stereotype.Controller
import java.time.format.DateTimeFormatter


@Controller
class ArticleController(val repository: ArticleRepository) : RouterFunction<ServerResponse> {

    private val frenchDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val englishDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override fun route(req: ServerRequest) = route(req) {
        accept(TEXT_HTML).apply {
            GET("/blog/") { findAllView(req) }
            GET("/blog/{id}") { findOneView(req) }
        }
        accept(APPLICATION_JSON).apply {
            GET("/api/blog/") { findAll() }
            GET("/api/blog/{id}") { findOne(req) }
        }
    }

    fun findOneView(req: ServerRequest) = repository.findOne(req.pathVariable("id")).then { a ->
        val languageTag = req.headers().header(HttpHeaders.ACCEPT_LANGUAGE).first()
        val language = Language.findByTag(languageTag)
        val model = mapOf(Pair("article", toArticleDto(a, language)))
        ok().render("article", model)
    }

    fun findAllView(req: ServerRequest) = repository.findAll().collectList().then { articles ->
        val languageTag = req.headers().header(HttpHeaders.ACCEPT_LANGUAGE).first()
        val language = Language.findByTag(languageTag)
        ok().render("articles",  mapOf(Pair("articles", articles.map { toArticleDto(it, language) })))
    }

    fun findOne(req: ServerRequest) = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findOne(req.pathVariable("id"))))

    fun findAll() = ok().contentType(APPLICATION_JSON_UTF8).body(
            fromPublisher(repository.findAll()))


    private fun toArticleDto(article: Article, language: Language) = ArticleDto(
            article.id,
            article.author,
            if (language == Language.ENGLISH) article.addedAt.format(englishDateFormatter) else article.addedAt.format(frenchDateFormatter),
            if (article.title != null) article.title[language]!! else "",
            if (article.headline != null) article.headline[language]!! else "",
            if (article.content != null) article.content[language]!! else "")

    class ArticleDto(
        val id: String?,
        val author: User,
        val addedAt: String,
        val title: String,
        val headline: String,
        val content: String
    ) {
        private val parser = Parser.builder().extensions(listOf(AutolinkExtension.create())).build()
        private val renderer = HtmlRenderer.builder().build()

        val htmlHeadline: String
            get() = renderer.render(parser.parse(headline))

        val htmlContent: String
            get() = renderer.render(parser.parse(content))

    }
}
