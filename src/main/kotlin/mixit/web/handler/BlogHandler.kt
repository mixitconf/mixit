package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.model.Language.*
import mixit.repository.PostRepository
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.http.MediaType.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*


@Component
class BlogHandler(val repository: PostRepository,
                  val userRepository: UserRepository,
                  val properties: MixitProperties) {

    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug"), req.language())
            .flatMap { post -> userRepository.findOne(post.authorId).flatMap { author ->
                    val model = mapOf(Pair("post", post.toDto(author, req.language())), Pair("title", "blog.post.title|${post.title[req.language()]}"))
                    ok().render("post", model)
                }
            }.switchIfEmpty(repository.findBySlug(req.pathVariable("slug"), if (req.language() == FRENCH) ENGLISH else FRENCH).flatMap {
                permanentRedirect("${properties.baseUri}${if (req.language() == ENGLISH) "/en" else ""}/blog/${it.slug[req.language()]}")
            })

    fun findAllView(req: ServerRequest) = repository.findAll(req.language())
            .collectList()
            .flatMap { posts -> userRepository.findMany(posts.map { it.authorId }).collectMap{ it.login }.flatMap { authors ->
                val model = mapOf(Pair("posts", posts.map { it.toDto(authors[it.authorId]!!, req.language()) }), Pair("title", "blog.title"))
                ok().render("blog", model)
            }}

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("id")))

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun redirect(req: ServerRequest) = repository.findOne(req.pathVariable("id")).flatMap {
        permanentRedirect("${properties.baseUri}/blog/${it.slug[req.language()]}")
    }

    fun feed(req: ServerRequest) = ok().contentType(APPLICATION_ATOM_XML).render("feed", mapOf(Pair("feed", repository.findAll(req.language()).collectList().map { it.toFeed(req.language(), "blog.feed.title", "/blog") })))

}

class PostDto(
        val id: String?,
        val slug: String,
        val author: User,
        val addedAt: String,
        val title: String,
        val headline: String,
        val content: String?
)

fun Post.toDto(author: User, language: Language) = PostDto(
        id,
        slug[language] ?: "",
        author,
        addedAt.formatDate(language),
        title[language] ?: "",
        headline[language] ?: "",
        if (content != null) content[language] else  null)

class Feed(
        val title: String,
        val link: String,
        val updated: String,
        val entries: List<FeedEntry>
)

class FeedEntry(
        val id: String,
        val title: String,
        val link: String,
        val updated: String
)

fun Post.toFeedEntry(language: Language) = FeedEntry(
        id!!,
        title[language]!!,
        slug[language]!!,
        addedAt.toRFC3339()
)

fun List<Post>.toFeed(language: Language, title: String, link: String)= Feed(
        title,
        link,
        first().addedAt.toRFC3339(),
        map { it.toFeedEntry(language) }
)