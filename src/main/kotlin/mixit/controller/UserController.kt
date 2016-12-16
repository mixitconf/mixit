package mixit.controller

import mixit.repository.UserRepository
import mixit.support.fromPublisher
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

class UserController(val repository: UserRepository) : RouterFunction<ServerResponse> {

    // TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions.route(
                GET("/user/{id}"), findViewById()).andRoute(
                GET("/user/"), findAllView()).andRoute(
                GET("/api/user/{id}"), findById()).andRoute(
                GET("/api/user/"), findAll()).route(request) as Mono<HandlerFunction<ServerResponse>>

    fun findViewById() = HandlerFunction { req ->
        repository.findById(req.pathVariable("id").toLong()).then{ u -> ok().render("user", mapOf(Pair("user", u))) }
    }

    fun findById() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findById(req.pathVariable("id").toLong())))
    }

    fun findAllView() = HandlerFunction {
        repository.findAll().collectList().then{ u -> ok().render("users",  mapOf(Pair("users", u))) }
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }
}