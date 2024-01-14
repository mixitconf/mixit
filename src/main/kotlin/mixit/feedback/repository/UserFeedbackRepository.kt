package mixit.feedback.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.feedback.model.UserFeedback
import mixit.talk.model.Talk
import mixit.user.repository.UserRepository
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
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.core.query.TextQuery
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository

@Repository
class UserFeedbackRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun count() =
        template.count<UserFeedback>()

    suspend fun findAll(): List<UserFeedback> =
        template
            .find<UserFeedback>(Query().with(by(Order(ASC, "start")))).doOnComplete { logger.info("Load all feedback") }
            .collectList()
            .awaitSingle()

    fun findOne(id: String) =
        template.findById<UserFeedback>(id)

    fun deleteOne(id: String) = template.remove<UserFeedback>(Query(where("_id").isEqualTo(id)))

    fun save(talk: UserFeedback) = template.save(talk)
}
