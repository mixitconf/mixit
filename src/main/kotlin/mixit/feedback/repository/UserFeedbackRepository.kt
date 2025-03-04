package mixit.feedback.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.feedback.model.UserFeedback
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Order
import org.springframework.data.domain.Sort.by
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository

@Repository
class UserFeedbackRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper,
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            (2024..CURRENT_EVENT.toInt()).forEach {
                loadYear(it)
            }
        }
    }

    private fun loadYear(year: Int) {
        ClassPathResource("data/feedback_$year.json").inputStream.use { resource ->
            val tickets: List<UserFeedback> = objectMapper.readValue(resource)
            tickets.forEach { save(it).block() }
            logger.info("Feedback initialization complete")
        }
    }

    fun count() =
        template.count<UserFeedback>()

    suspend fun findAll(): List<UserFeedback> =
        template
            .find<UserFeedback>(Query().with(by(Order(ASC, "start"))))
            .doOnComplete { logger.info("Load all feedback") }
            .collectList()
            .awaitSingle()

    suspend fun findAllByYear(year: String): List<UserFeedback> =
        template
            .find<UserFeedback>(Query(where("event").isEqualTo(year)).with(by(Order(ASC, "start"))))
            .doOnComplete { logger.info("Load all $year feedback") }
            .collectList()
            .awaitSingle()

    fun findOne(id: String) =
        template.findById<UserFeedback>(id)

    fun deleteOne(id: String) = template.remove<UserFeedback>(Query(where("_id").isEqualTo(id)))

    fun save(talk: UserFeedback) = template.save(talk)
}
