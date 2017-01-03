package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.SessionDataDto
import mixit.model.Session
import mixit.support.getEntityInformation
import org.springframework.core.io.ResourceLoader
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

class SessionRepository(db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory, val resourceLoader: ResourceLoader) :
        SimpleReactiveMongoRepository<Session, String>(f.getEntityInformation(Session::class), db) {


    fun initData() {
        deleteAll().block()

        val years = listOf(2012, 2013, 2014, 2015, 2016)
        years.forEach { year -> saveSessionForEvent(year) }
    }

    /**
     * Loads data from the json sponsor files
     */
    fun saveSessionForEvent(year: Int) {
        val filename = "classpath:data/session/session_".plus(year).plus(".json")
        val file = resourceLoader.getResource(filename)

        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        var sessions: List<SessionDataDto> = objectMapper.readValue(file.file)

        sessions
                .map { session -> session.toSession() }
                .forEach { session -> save(session).block() }
    }

}