package mixit.repository

import mixit.data.service.DataInitializer
import mixit.model.sponsor.Sponsor
import mixit.support.find
import mixit.support.findById
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class SponsorRepository(val db: ReactiveMongoTemplate, val initializer: DataInitializer) {

    fun init() {
        initializer.readSpeakers()
    }

    fun findById(id: Long) : Mono<Sponsor> = db.findById(id)

    fun findAll() : Flux<Sponsor> = db.find(Query().with(Sort("id")))
}