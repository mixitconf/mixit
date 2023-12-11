package mixit.blog.handler

import mixit.blog.repository.PostRepository
import org.springframework.web.reactive.function.server.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class JsonBlogHandler(val blogRepository: PostRepository) {
    suspend fun findOne(req: ServerRequest) =
        ok().json().bodyValueAndAwait(blogRepository.findOne(req.pathVariable("id")))

    suspend fun findAll(req: ServerRequest) =
        ok().json().bodyValueAndAwait(blogRepository.findAll())
}
