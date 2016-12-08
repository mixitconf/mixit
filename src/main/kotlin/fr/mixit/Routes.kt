package fr.mixit

import fr.mixit.handler.UserHandler
import org.springframework.core.io.ClassPathResource
import org.springframework.http.codec.BodyInserters.fromObject
import org.springframework.web.reactive.function.*
import org.springframework.web.reactive.function.RequestPredicates.GET
import org.springframework.web.reactive.function.RouterFunctions.resources
import org.springframework.web.reactive.function.RouterFunctions.route
import org.springframework.web.reactive.function.ServerResponse.ok

fun routes(): RouterFunction<*> {
    return route(GET("/"), HandlerFunction { ok().body(fromObject("Hello Mix-IT!")) })
            .and(UserHandler.routes())
            .and(resources("/**", ClassPathResource("static/")))
}