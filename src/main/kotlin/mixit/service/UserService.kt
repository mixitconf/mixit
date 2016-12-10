package mixit.service

import mixit.model.User
import io.requery.Persistable
import io.requery.kotlin.eq
import io.requery.sql.KotlinEntityDataStore

class UserService(val dataStore: KotlinEntityDataStore<Persistable>) {

    fun findById(id: Long) = dataStore.select(User::class) where (User::id eq id)

    fun findAll() = dataStore.select(User::class).get().toList()

}