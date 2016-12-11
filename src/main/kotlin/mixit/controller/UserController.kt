package mixit.controller

import mixit.service.UserService
import org.springframework.http.codec.BodyInserters.fromObject
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.web.reactive.function.RouterFunctions
import org.springframework.web.reactive.function.ServerRequest
import org.springframework.web.reactive.function.ServerResponse.ok
import java.util.*

class UserController(val service: UserService) : RouterFunction<Any> {

    // Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions.route(
                GET("/user/{id}"), findViewById()).andRoute(
                GET("/user/"), findAllView()).andRoute(
                GET("/api/user/{id}"), findById()).andRoute(
                GET("/api/user/"), findAll()).route(request) as Optional<HandlerFunction<Any>>

    fun findViewById() = HandlerFunction { req ->
        ok().render("user", mapOf(Pair("user", service.findById(req.pathVariable("id").toLong()))))
    }

    fun findById() = HandlerFunction { req ->
        ok().body(fromObject(service.findById(req.pathVariable("id").toLong())))
    }

    fun findAllView() = HandlerFunction {
        ok().render("users", mapOf(Pair("users", service.findAll())))
    }

    fun findAll() = HandlerFunction {
        ok().body(fromObject(service.findAll()))
    }
}