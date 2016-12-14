package mixit.support

import org.reactivestreams.Publisher
import org.springframework.beans.MutablePropertyValues
import org.springframework.beans.factory.SupplierFactoryBean
import org.springframework.beans.factory.config.ConstructorArgumentValues
import org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.support.AbstractApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.ResourcePropertySource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.codec.BodyInserter
import org.springframework.http.codec.BodyInserters
import org.springframework.web.client.reactive.ClientResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.util.function.Supplier
import kotlin.reflect.KClass

fun <T : Any> AbstractApplicationContext.getBean(type: KClass<T>) = getBean(type.java)

inline fun <reified T : Any> GenericApplicationContext.registerBean(supplier: Supplier<T>) {
    val beanName: String = T::class.simpleName!!
    registerBean(beanName, supplier)
}

inline fun <reified T : Any> GenericApplicationContext.registerBean(beanName: String, supplier: Supplier<T>) {
    val constructorArgumentValues = ConstructorArgumentValues()
    constructorArgumentValues.addIndexedArgumentValue(0, T::class.java)
    constructorArgumentValues.addIndexedArgumentValue(1, supplier)
    val factoryBeanDefinition = RootBeanDefinition(SupplierFactoryBean::class.java, constructorArgumentValues, MutablePropertyValues())
    registerBeanDefinition(beanName, factoryBeanDefinition)
}

fun GenericApplicationContext.registerBean(type: KClass<*>) {
    val beanName: String = type.simpleName!!
    val beanDefinition = RootBeanDefinition(type.java)
    beanDefinition.autowireMode = AUTOWIRE_CONSTRUCTOR
    registerBeanDefinition(beanName, beanDefinition)
}

fun ConfigurableEnvironment.addPropertySource(location: String) {
    propertySources.addFirst(ResourcePropertySource(EncodedResource(ClassPathResource(location), StandardCharsets.UTF_8)))
}

fun ClientResponse.bodyToFlux(type: KClass<*>) = bodyToFlux(type.java)

inline fun <reified T : Publisher<S>, reified S : Any> fromPublisher(publisher: T) : BodyInserter<T, ReactiveHttpOutputMessage> =
    BodyInserters.fromPublisher(publisher, S::class.java)

inline fun <reified T : Any> ClientResponse.bodyToFlux() = bodyToFlux(T::class.java)

inline fun <reified T : Any> ReactiveMongoTemplate.findById(id: Any) : Mono<T> = findById(id, T::class.java)

inline fun <reified T : Any> ReactiveMongoTemplate.find(query: Query) : Flux<T> = find(query, T::class.java)