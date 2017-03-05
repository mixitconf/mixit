package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Role
import mixit.model.User
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import mixit.util.*

@Repository
class UserRepository(val template: ReactiveMongoTemplate) {

    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        deleteAll().block()
        val usersResource = ClassPathResource("data/users.json")
        val users: List<User> = objectMapper.readValue(usersResource.inputStream)
        users.forEach { save(it).block() }
    }

    fun findByYear(year: Int): Flux<User> {
        val query = Query().addCriteria(Criteria.where("year").`is`(year))
        return template.find(query)
    }

    fun findByRole(role: Role): Flux<User> {
        val query = Query().addCriteria(Criteria.where("role").`is`(role))
        return template.find(query)
    }

    fun findByRoleAndEvent(role: Role, event: String): Flux<User> {
        val query = Query().addCriteria(Criteria.where("role").`is`(role).and("events").`in`(event))
        return template.find(query)
    }

    fun findOneByRole(login: String, role: Role): Mono<User> {
        val query = Query().addCriteria(Criteria.where("role").`in`(role).and("_id").`is`(login))
        return template.findOne(query)
    }

    fun findAll(): Flux<User> = template.findAll(User::class)

    fun findOne(id: String) = template.findById(id, User::class)

    fun findByLegacyId(id: Long): Mono<User> {
        val query = Query().addCriteria(Criteria.where("legacyId").`is`(id))
        return template.findOne(query)
    }

    fun deleteAll() = template.remove(Query(), User::class)

    fun save(user: User) = template.save(user)

    fun save(user: Mono<User>) = template.save(user)

}
