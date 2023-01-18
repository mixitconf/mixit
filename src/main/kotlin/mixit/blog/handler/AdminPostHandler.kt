package mixit.blog.handler

import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitApplication
import mixit.MixitProperties
import mixit.blog.model.BlogService
import mixit.blog.model.Post
import mixit.routes.MustacheI18n
import mixit.routes.MustacheTemplate.AdminBlog
import mixit.routes.MustacheTemplate.AdminPost
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
import org.springframework.web.reactive.function.server.renderAndAwait
import java.time.LocalDateTime

@Component
class AdminPostHandler(private val service: BlogService, private val properties: MixitProperties) {

    companion object {
        const val LIST_URI = "/admin/blog"
    }

    suspend fun adminBlog(req: ServerRequest): ServerResponse {
        val posts = service
            .findAll()
            .sortedByDescending { it.addedAt }
            .map { it.toDto(req.language()) }
        return ok().renderAndAwait(AdminBlog.template, mapOf(MustacheI18n.POSTS to posts))
    }

    suspend fun createPost(req: ServerRequest): ServerResponse =
        this.adminPost(null)

    suspend fun editPost(req: ServerRequest): ServerResponse =
        this.adminPost(service.findOneOrNull(req.pathVariable("id"))?.toPost()?.let {
            it.copy(year = it.year ?:  it.addedAt.year)
        })

    suspend fun adminDeletePost(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        service.deleteOne(formData["id"]!!).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    private suspend fun adminPost(post: Post?): ServerResponse {
        val blogpost = post ?: Post("", year = MixitApplication.CURRENT_EVENT.toInt())

        return ok().renderAndAwait(
            AdminPost.template,
            mapOf(
                Pair("post", blogpost),
                Pair("title-fr", blogpost.title[FRENCH]),
                Pair("title-en", blogpost.title[ENGLISH]),
                Pair("headline-fr", blogpost.headline[FRENCH]),
                Pair("headline-en", blogpost.headline[ENGLISH]),
                Pair("content-fr", blogpost.content?.get(FRENCH)),
                Pair("content-en", blogpost.content?.get(ENGLISH))
            )
        )
    }

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
            year = formData["year"]!!.toInt(),
            headline = mapOf(Pair(FRENCH, formData["headline-fr"]!!), Pair(ENGLISH, formData["headline-en"]!!)),
            content = mapOf(Pair(FRENCH, formData["content-fr"]!!), Pair(ENGLISH, formData["content-en"]!!))
        )
        service.save(post).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }
}
