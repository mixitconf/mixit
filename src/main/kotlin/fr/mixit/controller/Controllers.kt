package fr.mixit.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.web.reactive.function.RouterFunctions.resources

object Controllers {

    fun routes() = resources("/**", ClassPathResource("static/"))
            .and(UserController.routes())
}

