package mixit.event.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import mixit.event.model.Event
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class EventRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            ClassPathResource("data/events.json").inputStream.use { resource ->
                val events: List<Event> = objectMapper.readValue(resource)
                events.forEach { save(it).block(Duration.ofSeconds(10)) }
                logger.info("Events data initialization complete")
            }
        }
    }

    fun count() =
        template.count<Event>()

    suspend fun findAll() =
        template
            .find<Event>(Query().with(Sort.by("year"))).doOnComplete { logger.info("Load all events") }
            .collectList()
            .awaitSingle()

    suspend fun findOne(id: String) =
        template.findById<Event>(id).doOnSuccess { logger.info("Try to find event $id") }.awaitSingle()

    fun save(event: Event) =
        template.save(event)

    suspend fun findByYear(year: Int) =
        template.findOne<Event>(Query(Criteria.where("year").isEqualTo(year))).awaitSingle()
}
