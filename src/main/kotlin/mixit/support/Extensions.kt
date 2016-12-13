package mixit.support

import org.reactivestreams.Publisher
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.codec.BodyInserter
import org.springframework.http.codec.BodyInserters
import org.springframework.web.client.reactive.ClientResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

fun <T : Any> AbstractApplicationContext.getBean(type: KClass<T>) = getBean(type.java)

fun AnnotationConfigApplicationContext.register(type: KClass<*>) {
    register(type.java)
}

fun ClientResponse.bodyToFlux(type: KClass<*>) = bodyToFlux(type.java)

inline fun <reified T : Publisher<S>, reified S : Any> fromPublisher(publisher: T) : BodyInserter<T, ReactiveHttpOutputMessage> =
    BodyInserters.fromPublisher(publisher, S::class.java)

inline fun <reified T : Any> ClientResponse.bodyToFlux() = bodyToFlux(T::class.java)

inline fun <reified T : Any> ReactiveMongoTemplate.findById(id: Any) : Mono<T> = findById(id, T::class.java)

inline fun <reified T : Any> ReactiveMongoTemplate.find(query: Query) : Flux<T> = find(query, T::class.java)