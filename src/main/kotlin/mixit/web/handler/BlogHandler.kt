package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.model.Language.*
import mixit.repository.PostRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*


@Component
class BlogHandler(val repository: PostRepository,
                  val markdownConverter: MarkdownConverter,
                  val mixitProperties: MixitProperties) {

    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug"), req.language()).then { a ->
        val model = mapOf(Pair("post", a.toDto(req.language(), markdownConverter)))
        ok().render("post", model)
    }.otherwiseIfEmpty(repository.findBySlug(req.pathVariable("slug"), if (req.language() == FRENCH) ENGLISH else FRENCH).then { a ->
        permanentRedirect("${mixitProperties.baseUri}${if (req.language() == ENGLISH) "/en" else ""}/blog/${a.slug[req.language()]}")
    })

    fun findAllView(req: ServerRequest) = repository.findAll(req.language()).collectList().then { articles ->
        val model = mapOf(Pair("posts", articles.map { it.toDto(req.language(), markdownConverter) }))
        ok().render("blog", model)
    }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("id")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

}

class PostDto(
        val id: String?,
        val slug: String,
        val author: User,
        val addedAt: String,
        val title: String,
        val headline: String,
        val content: String
)

fun Post.toDto(language: Language, markdownConverter: MarkdownConverter) = PostDto(
        id,
        slug[language] ?: "",
        author,
        addedAt.format(language),
        title[language] ?: "",
        markdownConverter.toHTML(headline[language] ?: ""),
        markdownConverter.toHTML(if (content != null) content[language] else  ""))

class UserDto(
        val login: String,
        val firstname: String,
        val lastname: String,
        var email: String,
        var company: String? = null,
        var description: String,
        var logoUrl: String? = null,
        val events: List<String>,
        val role: Role,
        var links: List<Link>
)

fun User.toDto(language: Language, markdownConverter: MarkdownConverter) =
        UserDto(login, firstname, lastname, email, company, markdownConverter.toHTML(description[language] ?: ""),
                logoUrl, events, role, links)
