package mixit.blog.handler

import mixit.MixitProperties
import mixit.blog.model.BlogService
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate.Blog
import mixit.routes.MustacheTemplate.BlogPost
import mixit.util.errors.NotFoundException
import mixit.util.language
import mixit.util.permanentRedirect
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class WebBlogHandler(val service: BlogService, val properties: MixitProperties) {
    suspend fun findOneView(req: ServerRequest): ServerResponse {
        val post = service.findBySlug(req.pathVariable("slug")) ?: throw NotFoundException()
        val params = mapOf(
            TITLE to "blog.post.title|${post.title[req.language()]}",
            "post" to post.toDto(req.language())
        )
        return ok().renderAndAwait(BlogPost.template, params)
    }

    suspend fun findAllView(req: ServerRequest): ServerResponse {
        val posts = service.findAll()
        val params = mapOf(
            TITLE to Blog.title,
            "posts" to posts.sortedByDescending { it.addedAt }.map { it.toDto(req.language()) }
        )
        return ok().renderAndAwait(Blog.template, params)
    }

    suspend fun redirect(req: ServerRequest): ServerResponse {
        val post = service.findOneOrNull(req.pathVariable("id")) ?: throw NotFoundException()
        return permanentRedirect("${properties.baseUri}/blog/${post.slug[req.language()]}")
    }
}
