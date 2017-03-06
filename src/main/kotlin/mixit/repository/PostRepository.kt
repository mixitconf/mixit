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
import mixit.util.*
import org.springframework.data.mongodb.core.query.Criteria.*


@Repository
class PostRepository(val template: ReactiveMongoTemplate) {

    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        deleteAll().block()
        val blogResource = ClassPathResource("data/blog.json")
        val posts: List<Post> = objectMapper.readValue(blogResource.inputStream)
        posts.forEach { save(it).block() }
    }

    fun findOne(id: String) = template.findById<Post>(id)

    fun findBySlug(slug: String, lang: Language) =
            template.findOne<Post>(Query(where("slug.$lang").`is`(slug)))

    fun findAll(lang: Language? = null): Flux<Post> {
        val query = Query()
        query.with(Sort(Order(Direction.DESC, "addedAt")))
        query.fields().exclude("content")
        if (lang != null) {
            query.addCriteria(where("content.$lang").exists(true))
        }
        return template.find(query)
    }

    fun deleteAll() = template.remove<Post>(Query())

    fun save(article: Post) = template.save(article)

}
