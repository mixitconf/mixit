package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.SessionDataDto
import mixit.model.Session
import mixit.support.getEntityInformation
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import reactor.core.publisher.Flux


class SessionRepository(val db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory) :
        SimpleReactiveMongoRepository<Session, String>(f.getEntityInformation(Session::class), db) {


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
        var sessions: List<SessionDataDto> = objectMapper.readValue(file.file)

        sessions
                .map { session -> session.toSession() }
                .forEach { session -> save(session).block() }
    }

    fun findByEvent(eventId: String): Flux<Session> {
        val query = Query().addCriteria(Criteria.where("event").`is`(eventId))
        return db.find(query, Session::class.java)
    }
}
