package fr.mixit.handler

import fr.mixit.model.User
import org.springframework.http.codec.BodyInserters.fromObject
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunctions.route
import org.springframework.web.reactive.function.ServerResponse.ok

object UserHandler {

    fun routes() = route(GET("/user/{id}"), HandlerFunction { req -> ok().body(fromObject(User(req.pathVariable("id").toLong(), "Robert"))) })

}