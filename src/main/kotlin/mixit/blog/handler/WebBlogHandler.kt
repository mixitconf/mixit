package mixit.blog.handler

import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitProperties
import mixit.blog.model.BlogService
import mixit.blog.model.toFeed
import mixit.event.model.EventService
import mixit.routes.MustacheI18n.SPONSORS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheI18n.YEAR
import mixit.routes.MustacheI18n.YEAR_SELECTOR
import mixit.routes.MustacheTemplate.Blog
import mixit.routes.MustacheTemplate.BlogPost
import mixit.routes.MustacheTemplate.Feed
import mixit.user.model.UserService
import mixit.util.YearSelector
import mixit.util.errors.NotFoundException
import mixit.util.language
import mixit.util.permanentRedirect
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class WebBlogHandler(
    private val service: BlogService,
    private val userService: UserService,
    private val eventService: EventService,
    private val properties: MixitProperties
) {

    suspend fun findOneView(req: ServerRequest): ServerResponse {
        val slug = req.pathVariable("slug")
        if ((2012..CURRENT_EVENT.toInt()).any { slug == it.toString() }) {
            return findAllView(req, slug.toInt())
        }
        val post = service.findBySlug(slug) ?: throw NotFoundException()
        val params = mapOf(
            TITLE to "blog.post.title|${post.title[req.language()]}",
            "post" to post.toDto(req.language())
        )
        return ok().renderAndAwait(BlogPost.template, params)
    }

    suspend fun findAllView(req: ServerRequest, year: Int): ServerResponse {
        val posts = service.findAll()
        val event = eventService.findByYear(year)
        val params = mapOf(
            TITLE to Blog.title,
            SPONSORS to userService.loadSponsors(event),
            YEAR_SELECTOR to YearSelector.create(year, "blog"),
            YEAR to year,
            "posts" to posts.filter { year == it.year }
                .sortedByDescending { it.addedAt }
                .map { it.toDto(req.language()) }
        )
        return ok().renderAndAwait(Blog.template, params)
    }

    suspend fun redirect(req: ServerRequest): ServerResponse {
        val post = service.findOneOrNull(req.pathVariable("id")) ?: throw NotFoundException()
        return permanentRedirect("${properties.baseUri}/blog/${post.slug[req.language()]}")
    }

    suspend fun feed(req: ServerRequest): ServerResponse {
        val feeds = service.findAll().toFeed(req.language(), "blog.feed.title", "/blog")
        return ok()
            .contentType(MediaType.APPLICATION_ATOM_XML)
            .renderAndAwait(Feed.template, mapOf(Pair("feed", feeds)))
    }
}
