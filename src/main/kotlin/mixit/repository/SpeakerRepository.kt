package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.MemberDataDto
import mixit.model.Speaker
import mixit.support.getEntityInformation
import org.springframework.core.io.ResourceLoader
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import reactor.core.publisher.Flux

class SpeakerRepository(val db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory, val resourceLoader: ResourceLoader) :
        SimpleReactiveMongoRepository<Speaker, String>(f.getEntityInformation(Speaker::class), db) {


    fun initData() {
        deleteAll().block()

        val years = listOf(12, 13, 14, 15, 16)
        years.forEach { year -> saveSpeakersByYear(year) }
    }

    /**
     * Loads data from the json speaker files
     */
    private fun saveSpeakersByYear(year: Int) {
        val filename = "classpath:data/speaker/speaker_mixit".plus(year).plus(".json")
        val file = resourceLoader.getResource(filename)

        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        var speakers: List<MemberDataDto> = objectMapper.readValue(file.file)

        speakers
                .map { speaker -> speaker.toSpeaker(year) }
                .forEach { speaker -> save(speaker).block() }
    }

    fun findByYear(year: Int): Flux<Speaker> {
        val query = Query().addCriteria(Criteria.where("year").`is`(year))
        return db.find(query, Speaker::class.java)
    }
}
