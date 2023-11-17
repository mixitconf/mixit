package mixit.event.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import mixit.event.model.EventImages
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class EventImagesRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val eventsResource = ClassPathResource("data/events_image.json")
            val events: List<EventImages> = objectMapper.readValue(eventsResource.inputStream)
            events.forEach { save(it).block(Duration.ofSeconds(10)) }
            logger.info("Events images data initialization complete")
        }
    }

    fun count() =
        template.count<EventImages>()

    suspend fun findOne(id: String) =
        template.findById<EventImages>(id).doOnSuccess { logger.info("Try to find event $id") }.awaitSingle()

    suspend fun findAll() =
        template
            .find<EventImages>(Query().with(Sort.by("year"))).doOnComplete { logger.info("Load all event images") }
            .collectList()
            .awaitSingle()

    fun save(event: EventImages) =
        template.save(event)

    fun deleteAll() = template.remove<EventImages>(Query())
}
