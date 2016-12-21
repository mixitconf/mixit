package mixit.repository

import mixit.model.User
import mixit.support.getEntityInformation
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository


class UserRepository(db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory) :
        SimpleReactiveMongoRepository<User, String>(f.getEntityInformation(User::class), db) {

    fun initData() {
        deleteAll().block()
        save(User("robert", "Robert", "Foo")).block()
        save(User("raide", "Raide", "Bar")).block()
        save(User("ford", "Ford", "Baz")).block()
    }

}