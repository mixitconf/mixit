package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Talk
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import mixit.util.*
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria.*


@Repository
class TalkRepository(val template: ReactiveMongoTemplate) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        deleteAll().block()
        listOf(2012, 2013, 2014, 2015, 2016).forEach { year ->
            val talksResource = ClassPathResource("data/talks_$year.json")
            val talks: List<Talk> = objectMapper.readValue(talksResource.inputStream)
            talks.forEach { save(it).block() }
        }
        logger.info("Talks data initialization complete")
    }

    fun findByEvent(eventId: String) =
        template.find<Talk>(Query(where("event").`is`(eventId)))


    fun findAll(): Flux<Talk> = template.findAll<Talk>()

    fun findOne(id: String) = template.findById<Talk>(id)

    fun findBySlug(slug: String) =
            template.findOne<Talk>(Query(where("slug").`is`(slug)))

    fun deleteAll() = template.remove<Talk>(Query())

    fun save(talk: Talk) = template.save(talk)

}
