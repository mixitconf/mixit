package mixit.repository

import mixit.model.User
import mixit.support.find
import mixit.support.findById
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserRepository(val db: ReactiveMongoTemplate) {

    fun init() {
        db.insert(User(1L, "Robert")).subscribe()
        db.insert(User(2L, "Raide")).subscribe()
        db.insert(User(3L, "Ford")).subscribe()
    }

    fun findById(id: Long) : Mono<User> = db.findById(id)

    fun findAll() : Flux<User> = db.find(Query().with(Sort("id")))
}