package mixit.web.handler.admin

import java.time.LocalDateTime
import mixit.MixitProperties
import mixit.model.Language.ENGLISH
import mixit.model.Language.FRENCH
import mixit.model.Post
import mixit.model.User
import mixit.repository.PostRepository
import mixit.repository.UserRepository
import mixit.util.extractFormData
import mixit.util.language
import mixit.util.seeOther
import mixit.util.toSlug
import mixit.web.handler.blog.toDto
import mixit.web.handler.user.toDto
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AdminPostHandler(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val properties: MixitProperties
) {

    companion object {
        const val TEMPLATE_LIST = "admin-blog"
        const val TEMPLATE_EDIT = "admin-post"
        const val LIST_URI = "/admin/blog"
    }

    fun adminBlog(req: ServerRequest): Mono<ServerResponse> {
        val posts = postRepository
            .findAll()
            .collectList()
            .flatMap { posts ->
                userRepository
                    .findMany(posts.map { it.authorId })
                    .collectMap(User::login)
                    .map { authors ->
                        posts.map {
                            it.toDto(authors[it.authorId] ?: User(), req.language())
                        }
                    }
            }
        return ok().render(TEMPLATE_LIST, mapOf(Pair("posts", posts)))
    }

    fun createPost(req: ServerRequest): Mono<ServerResponse> =
        this.adminPost()

    fun editPost(req: ServerRequest): Mono<ServerResponse> =
        postRepository.findOne(req.pathVariable("id")).flatMap(this::adminPost)

    fun adminDeletePost(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            postRepository
                .deleteOne(formData["id"]!!)
                .then(seeOther("${properties.baseUri}$LIST_URI"))
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

    fun adminSavePost(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
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
            postRepository.save(post).then(seeOther("${properties.baseUri}$LIST_URI"))
        }

}
