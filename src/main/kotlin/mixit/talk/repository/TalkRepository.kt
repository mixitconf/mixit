package mixit.talk.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.talk.model.Talk
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Order
import org.springframework.data.domain.Sort.by
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.core.query.TextQuery
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class TalkRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            listOf(2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2021, 2022).forEach { year ->
                val talksResource = ClassPathResource("data/talks_$year.json")
                val talks: List<Talk> = objectMapper.readValue(talksResource.inputStream)
                talks.forEach { save(it).block() }
            }
            logger.info("Talks data initialization complete")
        }
    }

    fun count() =
        template.count<Talk>()

    fun findAll(): Flux<Talk> =
        template.find<Talk>(Query().with(by(Order(ASC, "start")))).doOnComplete { logger.info("Load all talks")  }

    fun findFullText(criteria: List<String>): Flux<Talk> {
        val textCriteria = TextCriteria()
        criteria.forEach { textCriteria.matching(it) }

        val query = TextQuery(textCriteria).sortByScore()
        return template.find(query)
    }

    fun findOne(id: String) =
        template.findById<Talk>(id)

    fun deleteAll() = template.remove<Talk>(Query())

    fun deleteOne(id: String) = template.remove<Talk>(Query(where("_id").isEqualTo(id)))

    fun deleteByEvent(event: String) = template.remove<Talk>(Query(where("event").isEqualTo(event)))

    fun save(talk: Talk) = template.save(talk)
}
