package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Session
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import mixit.support.*
import reactor.core.publisher.Mono

@Repository
class SessionRepository(val template: ReactiveMongoTemplate) {


    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        deleteAll().block()
        listOf(2012, 2013, 2014, 2015, 2016).forEach { year ->
            val sessionsResource = ClassPathResource("data/sessions_$year.json")
            val sessions: List<Session> = objectMapper.readValue(sessionsResource.inputStream)
            sessions.forEach { save(it).block() }
        }
    }

    fun findByEvent(eventId: String): Flux<Session> {
        val query = Query().addCriteria(Criteria.where("event").`is`(eventId))
        return template.find(query)
    }

    fun findAll(): Flux<Session> = template.findAll(Session::class)

    fun findOne(id: String) : Mono<Session>  = template.findById(id)

    fun findBySlug(slug: String) : Mono<Session> =
            template.findOne(Query().addCriteria(Criteria.where("slug").`is`(slug)))

    fun deleteAll() = template.remove(Query(), Session::class)

    fun save(session: Session) = template.save(session)
}
