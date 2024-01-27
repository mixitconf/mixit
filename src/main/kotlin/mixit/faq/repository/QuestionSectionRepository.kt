package mixit.faq.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.faq.model.QuestionSection
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.core.query.TextQuery
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class QuestionSectionRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        // TODO delete after index creation
        deleteAll().block()
        if (count().block() == 0L) {
            ClassPathResource("data/faq.json").inputStream.use { resource ->
                val sections: List<QuestionSection> = objectMapper.readValue(resource)
                sections.forEach {
                    saveInternal(it).block(Duration.ofSeconds(10))
                }
                logger.info("FAQ data initialization complete")
            }
        }
    }

    fun count() = template.count<QuestionSection>()

    suspend fun findAll(): List<QuestionSection> =
        template.findAll<QuestionSection>().collectList().awaitSingle()

    suspend fun findOneOrNull(id: String?): QuestionSection? =
        if (id == null) null else template.findById<QuestionSection>(id).awaitSingleOrNull()

    private fun saveInternal(section: QuestionSection) =
        template.save(section)
    suspend fun save(section: QuestionSection) =
        saveInternal(section).awaitSingle()

    fun deleteAll() =
        template.remove<QuestionSection>(Query())

    suspend fun deleteOne(id: String) =
        template.remove<QuestionSection>(Query(Criteria.where("_id").isEqualTo(id))).awaitSingle()

    suspend fun findFullText(criteria: List<String>): List<QuestionSection> {
        val textCriteria = TextCriteria()
        criteria.forEach { textCriteria.matching(it) }

        val query = TextQuery(textCriteria).sortByScore()
        return template.find<QuestionSection>(query).collectList().awaitSingle()
    }
}
