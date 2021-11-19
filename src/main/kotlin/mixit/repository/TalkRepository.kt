package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Talk
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.data.domain.Sort.Order
import org.springframework.data.domain.Sort.by
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.core.query.TextQuery
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux


@Repository
class TalkRepository(private val template: ReactiveMongoTemplate,
                     private val objectMapper: ObjectMapper) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            listOf(2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2021).forEach { year ->
                val talksResource = ClassPathResource("data/talks_$year.json")
                val talks: List<Talk> = objectMapper.readValue(talksResource.inputStream)
                talks.forEach { save(it).block() }
            }
            logger.info("Talks data initialization complete")
        }
    }

    fun count() = template.count<Talk>()

    fun findByEvent(eventId: String, topic: String? = null): Flux<Talk> {
        val criteria = where("event").isEqualTo(eventId)
        if (topic != null) criteria.and("topic").isEqualTo(topic)
        return template.find<Talk>(Query(criteria).with(by(Order(ASC, "start"))))
    }

    fun findByEventAndTalkIds(eventId: String, talkIds: List<String>, topic: String? = null): Flux<Talk> {
        val criteria = where("event").isEqualTo(eventId).and("id").inValues(talkIds)
        if (topic != null) criteria.and("topic").isEqualTo(topic)
        return template.find<Talk>(Query(criteria).with(by(Order(ASC, "start"))))
    }

    fun findBySpeakerId(speakerIds: List<String>, talkIdExcluded: String? = null): Flux<Talk> {
        val criteria = where("speakerIds").inValues(speakerIds)
        if (talkIdExcluded != null) criteria.and("id").ne(talkIdExcluded)
        return template.find<Talk>(Query(criteria).with(by(Order(DESC, "start"))))
    }

    fun findAll(): Flux<Talk> = template.find<Talk>(Query().with(by(Order(ASC, "start"))))

    fun findFullText(criteria: List<String>): Flux<Talk> {
        val textCriteria = TextCriteria()
        criteria.forEach { textCriteria.matching(it) }

        val query = TextQuery(textCriteria).sortByScore()
        return template.find(query)
    }

    fun findOne(id: String) = template.findById<Talk>(id)

    fun findBySlug(slug: String) =
            template.findOne<Talk>(Query(where("slug").isEqualTo(slug)))

    fun findByEventAndSlug(eventId: String, slug: String) =
            template.findOne<Talk>(Query(where("slug").isEqualTo(slug).and("event").isEqualTo(eventId)))

    fun deleteAll() = template.remove<Talk>(Query())

    fun deleteOne(id: String) = template.remove<Talk>(Query(where("_id").isEqualTo(id)))

    fun deleteByEvent(event: String) = template.remove<Talk>(Query(where("event").isEqualTo(event)))

    fun save(talk: Talk) = template.save(talk)

}
