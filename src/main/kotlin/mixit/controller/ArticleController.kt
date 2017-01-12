package mixit.controller

import mixit.repository.ArticleRepository
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.ServerResponse.ok

class ArticleController(val repository: ArticleRepository) : RouterFunction<ServerResponse> {

    override fun route(request: ServerRequest) = RouterFunctionDsl {
        accept(APPLICATION_JSON).apply {
            GET("/api/article/") { findAll() }
            GET("/api/article/{id}") { findOne() }
        }
    } (request)

    fun findOne() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findOne(req.pathVariable("id"))))
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }
}
