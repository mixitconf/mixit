package mixit.blog.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import mixit.blog.model.Post
import mixit.talk.model.Language
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.domain.Sort.Order
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.core.query.TextQuery
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository

@Repository
class PostRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

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

    suspend fun findOne(id: String) = template.findById<Post>(id).awaitSingle()

    fun findBySlug(slug: String, lang: Language) =
        template.findOne<Post>(Query(where("slug.$lang").isEqualTo(slug)))

    suspend fun findAll(lang: Language? = null): List<Post> {
        val query = Query()
        query.with(Sort.by(Order(DESC, "addedAt")))
        // query.fields().exclude("content")
        if (lang != null) {
            query.addCriteria(where("title.$lang").exists(true))
        }
        return template.find<Post>(query).doOnComplete { logger.info("Load all posts") }.collectList().awaitSingle()
    }

    suspend fun findFullText(criteria: List<String>): List<Post> {
        val textCriteria = TextCriteria()
        criteria.forEach { textCriteria.matching(it) }

        val query = TextQuery(textCriteria).sortByScore()
        return template.find<Post>(query).collectList().awaitSingle()
    }

    fun deleteAll() = template.remove<Post>(Query())

    fun deleteOne(id: String) = template.remove<Post>(Query(where("_id").isEqualTo(id)))

    fun save(article: Post) = template.save(article)
}
