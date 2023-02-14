package mixit.talk.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitApplication.Companion.CURRENT_EVENT
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
class TalkRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            (2012.. CURRENT_EVENT.toInt()).forEach { loadYear(it) }
        }
    }

    private fun loadYear(year: Int) {
        if(year == CURRENT_EVENT.toInt()) {
            logger.info("Refresh 2023 speaker data")
            userRepository.initSpeakerFor2023()
        }
        val talksResource = ClassPathResource("data/talks_$year.json")
        val talks: List<Talk> = objectMapper.readValue(talksResource.inputStream)
        talks.forEach { save(it).block() }
        logger.info("Talks data for $year initialization complete")
    }

    fun count() =
        template.count<Talk>()

    suspend fun findAll(): List<Talk> =
        template
            .find<Talk>(Query().with(by(Order(ASC, "start")))).doOnComplete { logger.info("Load all talks") }
            .collectList()
            .awaitSingle()

    suspend fun findFullText(criteria: List<String>): List<Talk> {
        val textCriteria = TextCriteria()
        criteria.forEach { textCriteria.matching(it) }

        val query = TextQuery(textCriteria).sortByScore()
        return template.find<Talk>(query).collectList().awaitSingle()
    }

    fun findOne(id: String) =
        template.findById<Talk>(id)

    fun deleteOne(id: String) = template.remove<Talk>(Query(where("_id").isEqualTo(id)))

    fun save(talk: Talk) = template.save(talk)
}
