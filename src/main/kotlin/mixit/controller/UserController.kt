package mixit.controller

import mixit.repository.UserRepository
import mixit.support.fromPublisher
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.*
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.ServerResponse.ok
import java.util.*

class UserController(val repository: UserRepository) : RouterFunction<Any> {

    // TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions.route(
                GET("/user/{id}"), findViewById()).andRoute(
                GET("/user/"), findAllView()).andRoute(
                GET("/api/user/{id}"), findById()).andRoute(
                GET("/api/user/"), findAll()).route(request) as Optional<HandlerFunction<Any>>

    // TODO Avoid using block() and perform non-blocking rendering of the view when https://jira.spring.io/browse/SPR-14870 will be fixed
    fun findViewById() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).render("user", mapOf(Pair("user", repository.findById(req.pathVariable("id").toLong()).block())))
    }

    fun findById() = HandlerFunction { req ->
        ok().body(fromPublisher(repository.findById(req.pathVariable("id").toLong())))
    }

    // TODO Avoid using block() and perform non-blocking rendering of the view when https://jira.spring.io/browse/SPR-14870 will be fixed
    fun findAllView() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).render("users", mapOf(Pair("users", repository.findAll().collectList().block())))
    }

    fun findAll() = HandlerFunction {
        ok().body(fromPublisher(repository.findAll()))
    }
}