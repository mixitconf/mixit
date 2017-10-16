package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.*
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository


@Repository
class EventRepository(private val template: ReactiveMongoTemplate,
                      private val objectMapper: ObjectMapper) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val eventsResource = ClassPathResource("data/events.json")
            val events: List<Event> = objectMapper.readValue(eventsResource.inputStream)
            events.forEach { save(it).block() }
            logger.info("Events data initialization complete")
        }
    }

    fun count() = template.count<Event>()

    fun findAll() = template.find<Event>(Query().with(Sort.by("year")))

    fun findOne(id: String) = template.findById<Event>(id)

    fun deleteAll() = template.remove<Event>(Query())

    fun save(event: Event) = template.save(event)

}
