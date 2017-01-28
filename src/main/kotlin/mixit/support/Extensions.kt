package mixit.support

import com.mongodb.client.result.DeleteResult
import org.springframework.boot.SpringApplication
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

fun run(type: KClass<*>, vararg args: String) = SpringApplication.run(type.java, *args)

inline fun <reified T : Any> ReactiveMongoOperations.findById(id: Any) : Mono<T> = findById(id, T::class.java)

fun <T : Any> ReactiveMongoOperations.findById(id: Any, type: KClass<T>) : Mono<T> = findById(id, type.java)

inline fun <reified T : Any> ReactiveMongoOperations.find(query: Query) : Flux<T> = find(query, T::class.java)

fun <T : Any> ReactiveMongoOperations.findAll(type: KClass<T>) : Flux<T> = findAll(type.java)

fun <T : Any> ReactiveMongoOperations.find(query: Query, type: KClass<T>) : Flux<T> = find(query, type.java)

inline fun <reified T : Any> ReactiveMongoOperations.findOne(query: Query) : Mono<T> = find(query, T::class.java).next()

fun ReactiveMongoOperations.remove(query: Query, type: KClass<*>): Mono<DeleteResult> = remove(query, type.java)
