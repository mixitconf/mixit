package mixit.blog.handler

import mixit.MixitProperties
import mixit.blog.repository.PostRepository
import mixit.talk.model.Language.ENGLISH
import mixit.talk.model.Language.FRENCH
import mixit.user.model.User
import mixit.user.repository.UserRepository
import mixit.util.json
import mixit.util.language
import mixit.util.permanentRedirect
import org.springframework.http.MediaType.APPLICATION_ATOM_XML
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body

@Component
class BlogHandler(
    val repository: PostRepository,
    val userRepository: UserRepository,
    val properties: MixitProperties
) {

    fun findOneView(req: ServerRequest) =
        repository
            .findBySlug(req.pathVariable("slug"), req.language())
            .flatMap { post ->
                userRepository.findOne(post.authorId).flatMap { author ->
                    val model = mapOf(
                        Pair("post", post.toDto(author, req.language())),
                        Pair("title", "blog.post.title|${post.title[req.language()]}")
                    )
                    ok().render("post", model)
                }
            }.switchIfEmpty(
                repository
                    .findBySlug(req.pathVariable("slug"), if (req.language() == FRENCH) FRENCH else ENGLISH)
                    .flatMap {
                        permanentRedirect("${properties.baseUri}${if (req.language() == ENGLISH) "/en" else ""}/blog/${it.slug[req.language()]}")
                    }
            )

    fun findAllView(req: ServerRequest) = repository.findAll(req.language())
        .collectList()
        .flatMap { posts ->
            userRepository
                .findMany(posts.map { it.authorId })
                .collectMap { it.login }
                .flatMap { authors ->
                    val dtos = posts.map { it.toDto(authors[it.authorId] ?: User(), req.language()) }
                    val model = mapOf(Pair("posts", dtos), Pair("title", "blog.title"))
                    ok().render("blog", model)
                }
        }

    fun findOne(req: ServerRequest) =
        ok().json().body(repository.findOne(req.pathVariable("id")))

    fun findAll(req: ServerRequest) =
        ok().json().body(repository.findAll())

    fun redirect(req: ServerRequest) =
        repository.findOne(req.pathVariable("id")).flatMap {
            permanentRedirect("${properties.baseUri}/blog/${it.slug[req.language()]}")
        }

    fun feed(req: ServerRequest) = ok().contentType(APPLICATION_ATOM_XML).render(
        "feed",
        mapOf(
            Pair(
                "feed",
                repository
                    .findAll(req.language())
                    .collectList()
                    .map { it.toFeed(req.language(), "blog.feed.title", "/blog") }
            )
        )
    )
}
