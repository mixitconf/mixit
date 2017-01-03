package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.MemberDataDto
import mixit.model.Event
import mixit.support.getEntityInformation
import org.springframework.core.io.ResourceLoader
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import reactor.core.publisher.Flux
import java.time.LocalDate

class EventRepository(db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory, val resourceLoader: ResourceLoader) :
        SimpleReactiveMongoRepository<Event, String>(f.getEntityInformation(Event::class), db) {


    fun initData() {
        deleteAll().block()

        val events = listOf(
                Event(2012, LocalDate.of(2012, 4, 26), LocalDate.of(2012, 4, 26), id = "2012"),
                Event(2013, LocalDate.of(2013, 4, 25), LocalDate.of(2013, 4, 26), id = "2013"),
                Event(2014, LocalDate.of(2014, 4, 29), LocalDate.of(2014, 4, 30), id = "2014"),
                Event(2015, LocalDate.of(2015, 4, 16), LocalDate.of(2015, 4, 17), id = "2015"),
                Event(2016, LocalDate.of(2016, 4, 21), LocalDate.of(2016, 4, 22), id = "2016")
        )

        events.listIterator().forEach { event -> save(readSponsorsForEvent(event)).block() }

        save(Event(2017, LocalDate.of(2017, 4, 20), LocalDate.of(2017, 4, 21), true, id = "2017")).block()
    }

    override fun findAll(): Flux<Event> = findAll(Sort("year"))

    /**
     * Loads data from the json sponsor files
     */
    fun readSponsorsForEvent(event: Event): Event {
        val filename = "classpath:data/sponsor_".plus(event.year).plus(".json")
        val file = resourceLoader.getResource(filename)

        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        var sponsors: List<MemberDataDto> = objectMapper.readValue(file.file)

        val ev = event.copy();
        ev.sponsors = sponsors.flatMap { sponsor -> sponsor.toEventSponsoring(event.year) }
        return ev;
    }

}