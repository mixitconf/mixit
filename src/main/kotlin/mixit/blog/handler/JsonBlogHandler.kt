package mixit.blog.handler

import mixit.blog.repository.PostRepository
import mixit.util.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body

@Component
class JsonBlogHandler(val blogRepository: PostRepository) {

    fun findOne(req: ServerRequest) =
        ok().json().body(blogRepository.findOne(req.pathVariable("id")))

    fun findAll(req: ServerRequest) =
        ok().json().body(blogRepository.findAll())

}
