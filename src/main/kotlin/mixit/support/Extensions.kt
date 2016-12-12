package mixit.support

import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.web.client.reactive.ClientResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

fun <T : Any> AbstractApplicationContext.getBean(type: KClass<T>) = getBean(type.java)

fun AnnotationConfigApplicationContext.register(type: KClass<*>) {
    register(type.java)
}

fun ClientResponse.bodyToFlux(type: KClass<*>) = bodyToFlux(type.java)

fun <T : Any> ReactiveMongoTemplate.findById(id: Any, type: KClass<T>) : Mono<T> = findById(id, type.java)

fun <T : Any> ReactiveMongoTemplate.find(query: Query, type: KClass<T>) : Flux<T> = find(query, type.java)