package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Talk
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import mixit.util.*
import reactor.core.publisher.Mono

@Repository
class TalkRepository(val template: ReactiveMongoTemplate) {


    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        deleteAll().block()
        listOf(2012, 2013, 2014, 2015, 2016).forEach { year ->
            val talksResource = ClassPathResource("data/talks_$year.json")
            val talks: List<Talk> = objectMapper.readValue(talksResource.inputStream)
            talks.forEach { save(it).block() }
        }
    }

    fun findByEvent(eventId: String): Flux<Talk> {
        val query = Query().addCriteria(Criteria.where("event").`is`(eventId))
        return template.find(query)
    }

    fun findAll(): Flux<Talk> = template.findAll(Talk::class)

    fun findOne(id: String) : Mono<Talk>  = template.findById(id)

    fun findBySlug(slug: String) : Mono<Talk> =
            template.findOne(Query().addCriteria(Criteria.where("slug").`is`(slug)))

    fun deleteAll() = template.remove(Query(), Talk::class)

    fun save(talk: Talk) = template.save(talk)
}
