package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.SessionDataDto
import mixit.model.Session
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import mixit.support.*

@Repository
class SessionRepository(val template: ReactiveMongoTemplate) {


    fun initData() {
        deleteAll().block()

        val years = listOf(12, 13, 14, 15, 16)
        years.forEach { year -> saveSessionsByYear(year) }
    }

    /**
     * Loads data from the json session files
     */
    private fun saveSessionsByYear(year: Int) {
        val file = ClassPathResource("data/session/session_mixit$year.json")

        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        var sessions: List<SessionDataDto> = objectMapper.readValue(file.inputStream)

        sessions
                .map { session -> session.toSession() }
                .forEach { session -> save(session).block() }
    }

    fun findByEvent(eventId: String): Flux<Session> {
        val query = Query().addCriteria(Criteria.where("event").`is`(eventId))
        return template.find(query)
    }

    fun findAll(): Flux<Session> = template.findAll(Session::class)

    fun findOne(id: String) = template.findById(id, Session::class)

    fun deleteAll() = template.remove(Query(), Session::class)

    fun save(session: Session) = template.save(session)
}
