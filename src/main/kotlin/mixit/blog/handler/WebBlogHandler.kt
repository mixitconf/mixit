package mixit.blog.handler

import mixit.MixitProperties
import mixit.blog.model.BlogService
import mixit.blog.model.toFeed
import mixit.util.language
import mixit.util.permanentRedirect
import org.springframework.http.MediaType.APPLICATION_ATOM_XML
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class WebBlogHandler(val service: BlogService, val properties: MixitProperties) {
    companion object {
        const val POST_LIST = "blog"
        const val POST_VIEW = "post"
        const val POST_FEED = "feed"
    }

    fun findOneView(req: ServerRequest): Mono<ServerResponse> {
        return service
            .findBySlug(req.pathVariable("slug"), req.language())
            .flatMap { post ->
                ok().render(
                    POST_VIEW,
                    mapOf(
                        Pair("post", post.toDto(req.language())),
                        Pair("title", "blog.post.title|${post.title[req.language()]}")
                    )
                )
            }
    }

    fun findAllView(req: ServerRequest): Mono<ServerResponse> =
        service
            .findAll()
            .flatMap { posts ->
                ok().render(
                    POST_LIST,
                    mapOf(
                        Pair("posts", posts.sortedByDescending { it.addedAt }.map { it.toDto(req.language()) }),
                        Pair("title", "blog.title")
                    )
                )
            }

    fun redirect(req: ServerRequest) =
        service.findOne(req.pathVariable("id")).flatMap {
            permanentRedirect("${properties.baseUri}/blog/${it.slug[req.language()]}")
        }

    fun feed(req: ServerRequest): Mono<ServerResponse> {
        val feeds = service.findAll().map { it.toFeed(req.language(), "blog.feed.title", "/blog") }
        return ok().contentType(APPLICATION_ATOM_XML).render(POST_FEED, mapOf(Pair("feed", feeds)))
    }
}
