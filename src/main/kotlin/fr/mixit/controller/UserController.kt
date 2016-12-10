package fr.mixit.controller

import fr.mixit.model.User
import fr.mixit.model.UserEntity
import org.springframework.http.codec.BodyInserters.fromObject
import org.springframework.web.reactive.function.HandlerFunction
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunctions.route
import org.springframework.web.reactive.function.ServerResponse.ok

object UserController {

    fun routes() = route(
                GET("/user/{id}"), findById()).andRoute(
                GET("/user/"), findAll())

    fun findById() = HandlerFunction { req ->
        val userEntity = UserEntity()
        userEntity.id = req.pathVariable("id").toLong()
        userEntity.name = "Robert"

        ok().body(fromObject(userEntity))
    }

    fun findAll()  = HandlerFunction {
        val user1 = UserEntity()
        user1.id = 1L
        user1.name = "Robert"

        val user2 = UserEntity()
        user2.id = 2L
        user2.name = "Raide"

        val user3 = UserEntity()
        user3.id = 3L
        user3.name = "Ford"

        ok().body(fromObject(listOf(user1, user2, user3)))
    }
}