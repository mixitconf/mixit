package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.*
import mixit.util.*
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class EventRepository(val template: ReactiveMongoTemplate, val userRepository: UserRepository) {


    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        deleteAll().block()
        val eventsResource = ClassPathResource("data/events.json")
        val events: List<Event> = objectMapper.readValue(eventsResource.inputStream)
        events.forEach { save(it).block() }
    }

    fun yearToId(year:String): String = "mixit${year.substring(2)}"

    fun findAll(): Flux<Event> = template.find(Query().with(Sort("year")), Event::class)

    fun findOne(id: String) = template.findById(id, Event::class)

    fun deleteAll() = template.remove(Query(), Event::class)

    fun save(event: Event) = template.save(event)


}
