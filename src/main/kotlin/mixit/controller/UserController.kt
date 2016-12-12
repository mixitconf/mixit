package mixit.controller

import mixit.model.User
import mixit.repository.UserRepository
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.http.codec.BodyInserters.fromPublisher
import org.springframework.web.reactive.function.*
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.ServerResponse.ok
import java.util.*

class UserController(val repository: UserRepository) : RouterFunction<Any> {

    // Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions.route(
                GET("/user/{id}"), findViewById()).andRoute(
                GET("/user/"), findAllView()).andRoute(
                GET("/api/user/{id}"), findById()).andRoute(
                GET("/api/user/"), findAll()).route(request) as Optional<HandlerFunction<Any>>

    fun findViewById() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).render("user", mapOf(Pair("user", repository.findById(req.pathVariable("id").toLong()).block())))
    }

    fun findById() = HandlerFunction { req ->
        ok().body(fromPublisher(repository.findById(req.pathVariable("id").toLong()), User::class.java))
    }

    fun findAllView() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).render("users", mapOf(Pair("users", repository.findAll().collectList().block())))
    }

    fun findAll() = HandlerFunction {
        ok().body(fromPublisher(repository.findAll(), User::class.java))
    }
}