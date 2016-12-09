package fr.mixit.controller

import fr.mixit.model.User
import org.springframework.http.codec.BodyInserters.fromObject
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunctions.route
import org.springframework.web.reactive.function.ServerResponse.ok

object UserController {

    fun routes() = route(
                GET("/user/{id}"), findById()).andRoute(
                GET("/user/"), findAll())

    fun findById() = HandlerFunction { req -> ok().body(fromObject(User(req.pathVariable("id").toLong(), "Robert"))) }
    fun findAll()  = HandlerFunction { ok().body(fromObject(listOf(User(1, "Foo"), User(2, "Bar"), User(3, "Baz")))) }
}