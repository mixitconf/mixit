package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Role
import mixit.model.User
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import mixit.util.*
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria.*


@Repository
class UserRepository(val template: ReactiveMongoTemplate,
                     val objectMapper: ObjectMapper) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val usersResource = ClassPathResource("data/users.json")
            val users: List<User> = objectMapper.readValue(usersResource.inputStream)
            users.forEach { save(it).block() }
            logger.info("Users data initialization complete")
        }
    }

    fun count() = template.count<User>()

    fun findByYear(year: Int) =
            template.find<User>(Query(where("year").`is`(year)))


    fun findByRole(role: Role) =
            template.find<User>(Query(where("role").`is`(role)))


    fun findByRoleAndEvent(role: Role, event: String) =
            template.find<User>(Query(where("role").`is`(role).and("events").`in`(event)))


    fun findOneByRole(login: String, role: Role) =
        template.findOne<User>(Query(where("role").`in`(role).and("_id").`is`(login)))


    fun findAll() = template.findAll<User>()

    fun findOne(login: String) = template.findById<User>(login)

    fun findMany(logins: List<String>) = template.find<User>(Query(where("_id").`in`(logins)))

    fun findByLegacyId(id: Long) =
            template.findOne<User>(Query(where("legacyId").`is`(id)))

    fun deleteAll() = template.remove<User>(Query())

    fun deleteOne(login: String) = template.remove<User>(Query(where("_id").`is`(login)))

    fun save(user: User) = template.save(user)

    fun save(user: Mono<User>) = template.save(user)

}
