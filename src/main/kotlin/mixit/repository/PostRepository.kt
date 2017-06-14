package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Post
import mixit.model.Language
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Order
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import mixit.util.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort.Direction.*
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria.*


@Repository
class PostRepository(val template: ReactiveMongoTemplate,
                     val objectMapper: ObjectMapper) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val blogResource = ClassPathResource("data/blog.json")
            val posts: List<Post> = objectMapper.readValue(blogResource.inputStream)
            posts.forEach { save(it).block() }
            logger.info("Blog posts data initialization complete")
        }
    }

    fun count() = template.count<Post>()

    fun findOne(id: String) = template.findById<Post>(id)

    fun findBySlug(slug: String, lang: Language) =
            template.findOne<Post>(Query(where("slug.$lang").`is`(slug)))

    fun findAll(lang: Language? = null): Flux<Post> {
        val query = Query()
        query.with(Sort.by(Order(DESC, "addedAt")))
        query.fields().exclude("content")
        if (lang != null) {
            query.addCriteria(where("title.$lang").exists(true))
        }
        return template.find(query)
    }

    fun deleteAll() = template.remove<Post>(Query())

    fun deleteOne(id: String) = template.remove<Post>(Query(where("_id").`is`(id)))

    fun save(article: Post) = template.save(article)

}
