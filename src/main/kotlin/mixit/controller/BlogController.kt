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
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.stream.Collectors.*
import java.util.stream.IntStream
import java.time.temporal.ChronoField
import java.time.format.DateTimeFormatterBuilder


@Controller
class BlogController(val repository: PostRepository,
                     val markdownConverter: MarkdownConverter,
                     @Value("\${baseUri}") val baseUri: String) : RouterFunctionProvider() {

    private val daysLookup : Map<Long, String> = IntStream.rangeClosed(1, 31).boxed().collect(toMap(Int::toLong, { i -> getOrdinal(i)}))
    private val frenchDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)
    private val englishDateFormatter = DateTimeFormatterBuilder()
            .appendPattern("MMMM")
            .appendLiteral(" ")
            .appendText(ChronoField.DAY_OF_MONTH, daysLookup)
            .appendLiteral(" ")
            .appendPattern("yyyy").toFormatter(Locale.ENGLISH)

    // TODO Remove this@ArticleController when KT-15667 will be fixed
    override val routes: Routes = {
        accept(TEXT_HTML).route {
            "/blog".route {
                GET("/", this@BlogController::findAllView)
                GET("/{slug}", this@BlogController::findOneView)
            }
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


    private fun Post.toDto(language: Language, markdownConverter: MarkdownConverter) = ArticleDto(
            id,
            slug[language] ?: "",
            author,
            if (language == ENGLISH) addedAt.format(englishDateFormatter) else addedAt.format(frenchDateFormatter),
            title[language] ?: "",
            markdownConverter.toHTML(headline[language] ?: ""),
            markdownConverter.toHTML(if (content != null) content[language] else  ""))

    class ArticleDto(
        val id: String?,
        val slug: String,
        val author: User,
        val addedAt: String,
        val title: String,
        val headline: String,
        val content: String
    )

    private fun getOrdinal(n: Int): String {
        if (n >= 11 && n <= 13) {
            return n.toString() + "th"
        }
        when (n % 10) {
            1 -> return n.toString() + "st"
            2 -> return n.toString() + "nd"
            3 -> return n.toString() + "rd"
            else -> return n.toString() + "th"
        }
    }
}
