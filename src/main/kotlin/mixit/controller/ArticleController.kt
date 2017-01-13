package mixit.controller

import mixit.model.Article
import mixit.model.Language
import mixit.model.User
import mixit.repository.ArticleRepository
import org.commonmark.parser.Parser
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.time.LocalDateTime
import org.commonmark.renderer.html.HtmlRenderer


class ArticleController(val repository: ArticleRepository) : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = RouterFunctionDsl {
        accept(TEXT_HTML).apply {
            GET("/article/") { findAllView() }
            GET("/article/{id}") { findOneView() }
        }
        accept(APPLICATION_JSON).apply {
            GET("/api/article/") { findAll() }
            GET("/api/article/{id}") { findOne() }
        }
    } (request)

    fun findOneView() = HandlerFunction { req ->
        repository.findOne(req.pathVariable("id")).then { a ->
            val languageTag = req.headers().header(HttpHeaders.ACCEPT_LANGUAGE).first()
            val language = Language.findByTag(languageTag)
            val model = mapOf(Pair("article", toArticleDto(a, language)))
            ok().render("article", model)
        }
    }

    fun findAllView() = HandlerFunction { req ->
        repository.findAll().collectList().then { articles ->
            val languageTag = req.headers().header(HttpHeaders.ACCEPT_LANGUAGE).first()
            val language = Language.findByTag(languageTag)
            ok().render("articles",  mapOf(Pair("articles", articles.map { toArticleDto(it, language) })))
        }
    }

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("id"))))
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }

    private fun toArticleDto(article: Article, language: Language) = ArticleDto(
            article.id,
            article.author,
            article.addedAt,
            article.contents[language]!!.title,
            article.contents[language]!!.content)

    class ArticleDto(
        val id: String?,
        val author: User,
        val addedAt: LocalDateTime,
        val title: String,
        val content: String
    ) {
        val htmlContent: String
            get() {
                val parser = Parser.builder().build()
                val document = parser.parse(content)
                val renderer = HtmlRenderer.builder().build()
                return renderer.render(document)
        }
    }
}
