package mixit.repository

import mixit.model.Event
import mixit.support.getEntityInformation
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import reactor.core.publisher.Flux
import java.time.LocalDate

class EventRepository(db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory) :
        SimpleReactiveMongoRepository<Event, String>(f.getEntityInformation(Event::class), db) {


    fun initData() {
        deleteAll().block()

        val events = listOf(
                Event("mixit12", LocalDate.of(2012, 4, 26), LocalDate.of(2012, 4, 26)),
                Event("mixit13", LocalDate.of(2013, 4, 25), LocalDate.of(2013, 4, 26)),
                Event("mixit14", LocalDate.of(2014, 4, 29), LocalDate.of(2014, 4, 30)),
                Event("mixit15", LocalDate.of(2015, 4, 16), LocalDate.of(2015, 4, 17)),
                Event("mixit16", LocalDate.of(2016, 4, 21), LocalDate.of(2016, 4, 22)),
                Event("mixit17", LocalDate.of(2017, 4, 20), LocalDate.of(2017, 4, 21), true)
        )
        events.forEach { event -> save(event).block() }
    }

    override fun findAll(): Flux<Event> = findAll(Sort("year"))

}
