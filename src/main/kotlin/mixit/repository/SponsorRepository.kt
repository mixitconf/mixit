package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.MemberDataDto
import mixit.model.Sponsor
import mixit.support.getEntityInformation
import org.springframework.core.io.ResourceLoader
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

class SponsorRepository(db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory, val resourceLoader: ResourceLoader) :
        SimpleReactiveMongoRepository<Sponsor, String>(f.getEntityInformation(Sponsor::class), db) {

    fun initData() {
        deleteAll().block()
        save(readSpeakers()).blockLast()
    }

    /**
     * Loads data from a json sponsor file
     */
    private fun readSpeaker(filename: String) : Iterable<Sponsor>{
        val file = resourceLoader.getResource(filename)
        val objectMapper : ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        var sponsors: List<MemberDataDto> = objectMapper.readValue(file.file)
        return sponsors.map { sponsor -> sponsor.toSponsor() }
    }

    /**
     * Loads data from the json sponsor files
     */
    fun readSpeakers() : Iterable<Sponsor>{
        val files = emptyList<String>()

        return files.flatMap { filename -> readSpeaker(filename) }
    }

}
