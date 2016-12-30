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
        save(Event(2012, LocalDate.of(2012, 4, 26), LocalDate.of(2012, 4, 26))).block()
        save(Event(2013, LocalDate.of(2013, 4, 25), LocalDate.of(2013, 4, 26))).block()
        save(Event(2014, LocalDate.of(2014, 4, 29), LocalDate.of(2014, 4, 30))).block()
        save(Event(2015, LocalDate.of(2015, 4, 16), LocalDate.of(2015, 4, 17))).block()
        save(Event(2016, LocalDate.of(2016, 4, 21), LocalDate.of(2016, 4, 22))).block()
        save(Event(2017, LocalDate.of(2017, 4, 20), LocalDate.of(2017, 4, 21), true)).block()
    }

    override fun findAll() : Flux<Event> = findAll(Sort("year"))
}