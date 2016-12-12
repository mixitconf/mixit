package mixit.repository

import mixit.model.User
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query

class UserRepository(val db: ReactiveMongoTemplate) {

    fun init() {
        db.insert(User(1L, "Robert")).subscribe()
        db.insert(User(2L, "Raide")).subscribe()
        db.insert(User(3L, "Ford")).subscribe()
    }

    fun findById(id: Long) = db.findById(id, User::class.java)

    fun findAll() = db.find(Query().with(Sort("id")), User::class.java)

}