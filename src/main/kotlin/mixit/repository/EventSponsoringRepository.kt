package mixit.repository

import mixit.model.sponsor.Event
import mixit.support.find
import mixit.support.findById
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

class EventSponsoringRepository(val db: ReactiveMongoTemplate) {

    fun init() {
        db.insert(Event(1L, 2012, false, Instant.ofEpochMilli(1461650400000), Instant.ofEpochMilli(1461686400000))).subscribe()
        db.insert(Event(2L, 2013, false, Instant.ofEpochMilli(1461564000000), Instant.ofEpochMilli(1461650400000))).subscribe()
        db.insert(Event(3L, 2014, false, Instant.ofEpochMilli(1461909600000), Instant.ofEpochMilli(1461996000000))).subscribe()
        db.insert(Event(4L, 2015, false, Instant.ofEpochMilli(1460786400000), Instant.ofEpochMilli(1460872800000))).subscribe()
        db.insert(Event(4L, 2016, false, Instant.ofEpochMilli(1461218400000), Instant.ofEpochMilli(1461304800000))).subscribe()
        db.insert(Event(4L, 2017, true, Instant.ofEpochMilli(1461132000000), Instant.ofEpochMilli(1461261600000))).subscribe()
    }

    fun findById(id: Long) : Mono<Event> = db.findById(id)

    fun findAll() : Flux<Event> = db.find(Query().with(Sort("year")))
}