package mixit.blog.handler

import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitProperties
import mixit.blog.model.BlogService
import mixit.blog.model.Post
import mixit.talk.model.Language.ENGLISH
import mixit.talk.model.Language.FRENCH
import mixit.util.extractFormData
import mixit.util.language
import mixit.util.seeOther
import mixit.util.toSlug
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Component
class AdminPostHandler(private val service: BlogService, private val properties: MixitProperties) {

    companion object {
        const val TEMPLATE_LIST = "admin-blog"
        const val TEMPLATE_EDIT = "admin-post"
        const val LIST_URI = "/admin/blog"
    }

    fun adminBlog(req: ServerRequest): Mono<ServerResponse> {
        val posts = service
            .findAll()
            .map { posts -> posts.sortedByDescending { it.addedAt }.map { it.toDto(req.language()) } }
        return ok().render(TEMPLATE_LIST, mapOf(Pair("posts", posts)))
    }

    fun createPost(req: ServerRequest): Mono<ServerResponse> =
        this.adminPost()

    fun editPost(req: ServerRequest): Mono<ServerResponse> =
        service.findOne(req.pathVariable("id")).map { it.toPost() }.flatMap(this::adminPost)

    suspend fun adminDeletePost(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        service.deleteOne(formData["id"]!!).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    private fun adminPost(post: Post = Post("")) = ok().render(
        TEMPLATE_EDIT,
        mapOf(
            Pair("post", post),
            Pair("title-fr", post.title[FRENCH]),
            Pair("title-en", post.title[ENGLISH]),
            Pair("headline-fr", post.headline[FRENCH]),
            Pair("headline-en", post.headline[ENGLISH]),
            Pair("content-fr", post.content?.get(FRENCH)),
            Pair("content-en", post.content?.get(ENGLISH))
        )
    )

    suspend fun adminSavePost(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val post = Post(
            id = formData["id"],
            addedAt = LocalDateTime.parse(formData["addedAt"]),
            authorId = formData["authorId"]!!,
            title = mapOf(Pair(FRENCH, formData["title-fr"]!!), Pair(ENGLISH, formData["title-en"]!!)),
            slug = mapOf(
                Pair(FRENCH, formData["title-fr"]!!.toSlug()),
                Pair(ENGLISH, formData["title-en"]!!.toSlug())
            ),
            headline = mapOf(Pair(FRENCH, formData["headline-fr"]!!), Pair(ENGLISH, formData["headline-en"]!!)),
            content = mapOf(Pair(FRENCH, formData["content-fr"]!!), Pair(ENGLISH, formData["content-en"]!!))
        )
        service.save(post).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }
}
