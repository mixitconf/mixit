package mixit.event.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Duration
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.event.model.EventImages
import mixit.talk.handler.TalkViewConfig.Companion.talks
import mixit.talk.model.Talk
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import kotlin.collections.forEach

@Repository
class EventImagesRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
            (2012..CURRENT_EVENT.toInt()).forEach {
                if (count(it.toString()).block() == 0L){
                    loadYear(it)
                }
            }
    }

    private fun loadYear(year: Int) {
        ClassPathResource("data/events_image_$year.json").inputStream.use { resource ->
            val events: List<EventImages> = objectMapper.readValue(resource)
            events.forEach { save(it).block(Duration.ofSeconds(10)) }
            logger.info("Events images data initialization complete for $year")
        }
    }

    fun count(event: String) =
        template.count<EventImages>(
            Query(Criteria.where("event").isEqualTo(event))
        )

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

    fun delete(event: String) = template.remove<EventImages>(
        Query(Criteria.where("event").isEqualTo(event))
    )

    fun deleteAll() = template.remove<EventImages>(Query())
}
