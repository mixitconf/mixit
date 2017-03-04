package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Post
import mixit.model.Language
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Order
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import mixit.support.*
import org.springframework.data.mongodb.core.query.Criteria.*
import reactor.core.publisher.Mono


@Repository
class PostRepository(val template: ReactiveMongoTemplate) {

    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        deleteAll().block()
        val articlesResource = ClassPathResource("data/articles.json")
        val articles: List<Post> = objectMapper.readValue(articlesResource.inputStream)
        articles.forEach { save(it).block() }
    }

    fun findOne(id: String) = template.findById(id, Post::class)

    fun findBySlug(slug: String, lang: Language) : Mono<Post> =
            template.findOne(Query().addCriteria(where("slug.$lang").`is`(slug)))

    fun findAll(lang: Language? = null): Flux<Post> {
        val query = Query()
        query.with(Sort(Order(Direction.DESC, "addedAt")))
        query.fields().exclude("content")
        if (lang != null) {
            query.addCriteria(where("content.$lang").exists(true))
        }
        return template.find(query)
    }

    fun deleteAll() = template.remove(Query(), Post::class)

    fun save(article: Post) = template.save(article)

}


