package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.model.Language.*
import mixit.repository.PostRepository
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*


@Component
class BlogHandler(val repository: PostRepository,
                  val userRepository: UserRepository,
                  val markdownConverter: MarkdownConverter,
                  val properties: MixitProperties) {

    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug"), req.language())
            .flatMap { post -> userRepository.findOne(post.authorId).flatMap { author ->
                    val model = mapOf(Pair("post", post.toDto(author, req.language(), markdownConverter)), Pair("title", "blog.post.title|${post.title[req.language()]}"))
                    ok().render("post", model)
                }
            }.otherwiseIfEmpty(repository.findBySlug(req.pathVariable("slug"), if (req.language() == FRENCH) ENGLISH else FRENCH).flatMap {
                permanentRedirect("${properties.baseUri}${if (req.language() == ENGLISH) "/en" else ""}/blog/${it.slug[req.language()]}")
            })

    fun findAllView(req: ServerRequest) = repository.findAll(req.language())
            .collectList()
            .flatMap { posts -> userRepository.findMany(posts.map { it.authorId }).collectMap{ it.login }.flatMap { authors ->
                val model = mapOf(Pair("posts", posts.map { it.toDto(authors[it.authorId]!!, req.language(), markdownConverter) }), Pair("title", "blog.title"))
                ok().render("blog", model)
            }}

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("id")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun redirect(req: ServerRequest) = repository.findOne(req.pathVariable("id")).flatMap {
        permanentRedirect("${properties.baseUri}/blog/${it.slug[req.language()]}")
    }

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

fun Post.toDto(author: User, language: Language, markdownConverter: MarkdownConverter) = PostDto(
        id,
        slug[language] ?: "",
        author,
        addedAt.formatDate(language),
        title[language] ?: "",
        markdownConverter.toHTML(headline[language] ?: ""),
        markdownConverter.toHTML(if (content != null) content[language] else  ""))
