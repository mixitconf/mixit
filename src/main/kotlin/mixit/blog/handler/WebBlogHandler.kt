package mixit.blog.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitProperties
import mixit.blog.model.BlogService
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate
import mixit.util.errors.NotFoundException
import mixit.util.language
import mixit.util.permanentRedirect
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok

@Component
class WebBlogHandler(val service: BlogService, val properties: MixitProperties) {
    suspend fun findOneView(req: ServerRequest): ServerResponse {
        val post = service.findBySlug(req.pathVariable("slug")) ?: throw NotFoundException()
        val params = mapOf(
            TITLE to "blog.post.title|${post.title[req.language()]}",
            "post" to post.toDto(req.language())
        )
        return ok().render(MustacheTemplate.BlogPost.template, params).awaitSingle()
    }

    suspend fun findAllView(req: ServerRequest): ServerResponse {
        val posts = service.coFindAll()
        val params = mapOf(
            TITLE to "blog.title",
            "posts" to posts.sortedByDescending { it.addedAt }.map { it.toDto(req.language()) }
        )
        return ok().render(MustacheTemplate.Blog.template, params).awaitSingle()
    }

    suspend fun redirect(req: ServerRequest): ServerResponse {
        val post = service.coFindOne(req.pathVariable("id"))
        return permanentRedirect("${properties.baseUri}/blog/${post.slug[req.language()]}")
    }

}
