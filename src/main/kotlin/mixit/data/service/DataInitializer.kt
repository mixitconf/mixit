package mixit.data.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.MemberDataDto
import mixit.model.sponsor.Sponsor
import org.springframework.core.io.ResourceLoader
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 20/12/16.
 */
class DataInitializer(val resourceLoader: ResourceLoader) {

    val filePath:String = "classpath:data/"

    /**
     * Loads data from a json sponsor file
     */
    private fun readSpeaker(filename: String) : Iterable<Sponsor>{
        val file = resourceLoader.getResource(filePath + filename)
        val objectMapper : ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        var sponsors: List<MemberDataDto> = objectMapper.readValue(file.file)
        return sponsors.map { sponsor -> sponsor.toSponsor() }
    }

    /**
     * Loads data from the json sponsor files
     */
    fun readSpeakers() : Iterable<Sponsor>{
       val files = arrayOf(
               "sponsor/sponsor_2012.json",
               "sponsor/sponsor_2013.json",
               "sponsor/sponsor_2014.json",
               "sponsor/sponsor_2015.json",
               "sponsor/sponsor_2016.json")

        return files.flatMap { filename -> readSpeaker(filename) }
    }
}