package mixit.controller

import mixit.model.User
import mixit.repository.UserRepository
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.web.reactive.function.BodyInserters.fromObject
import org.springframework.web.reactive.function.BodyInsertersExtension.fromPublisher
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.ServerRequestExtension.bodyToMono
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.net.URI

class UserController(val repository: UserRepository) : RouterFunction<ServerResponse> {

    @Suppress("UNCHECKED_CAST")// TODO Relax generics check to avoid explicit casting
    override fun route(request: ServerRequest) = RouterFunctions.route(
                GET("/user/{id}"), findViewById()).andRoute(
                GET("/user/"), findAllView()).andRoute(
                GET("/api/user/{id}"), findById()).andRoute(
                GET("/api/user/"), findAll()).andRoute(
                POST("/api/user/"), create()
    ).route(request) as Mono<HandlerFunction<ServerResponse>>

    fun findViewById() = HandlerFunction { req ->
        repository.findOne(req.pathVariable("id"))
                .then{ u -> ok().render("user", mapOf(Pair("user", u))) }
    }

    fun findById() = HandlerFunction { req ->
        ok().contentType(APPLICATION_JSON_UTF8).body(
                fromPublisher(repository.findOne(req.pathVariable("id"))))
    }

    fun findAllView() = HandlerFunction {
        repository.findAll()
                .collectList()
                .then{ u -> ok().render("users",  mapOf(Pair("users", u))) }
    }

    fun findAll() = HandlerFunction {
        ok().contentType(APPLICATION_JSON_UTF8).body(fromPublisher(repository.findAll()))
    }

    fun create() = HandlerFunction { req ->
        repository.save(req.bodyToMono(User::class)).single()
                .then{u -> created(URI.create("/api/user/${u.id}"))
                .contentType(APPLICATION_JSON_UTF8)
                .body(fromObject(u))}
    }
}