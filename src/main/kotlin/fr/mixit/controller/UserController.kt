package fr.mixit.controller

import fr.mixit.service.UserService
import org.springframework.http.codec.BodyInserters.fromObject
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunction
import org.springframework.web.reactive.function.RouterFunctions
import org.springframework.web.reactive.function.ServerRequest
import org.springframework.web.reactive.function.ServerResponse.ok
import java.util.*

class UserController(val service: UserService) : RouterFunction<Any> {

    // Relax Generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions.route(
                GET("/user/{id}"), findById()).andRoute(
                GET("/user/"), findAll()).route(request) as Optional<HandlerFunction<Any>>

    fun findById() = HandlerFunction { req ->
        ok().body(fromObject(service.findById(req.pathVariable("id").toLong())))
    }

    fun findAll() = HandlerFunction {
        ok().body(fromObject(service.findAll()))
    }
}