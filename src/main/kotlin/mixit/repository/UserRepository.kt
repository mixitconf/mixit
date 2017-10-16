package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Role
import mixit.model.User
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria.*
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo


@Repository
class UserRepository(private val template: ReactiveMongoTemplate,
                     private val objectMapper: ObjectMapper) {

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
            template.find<User>(Query(where("year").isEqualTo(year)))

    fun findByEmail(email: String) = template.findOne<User>(Query(where("email").isEqualTo(email)))

    fun findByName(firstname: String, lastname: String) =
            template.find<User>(Query(where("firstname").isEqualTo(firstname).and("lastname").isEqualTo(lastname)))

    fun findByRoles(roles: List<Role>) =
            template.find<User>(Query(where("role").inValues(roles)))


    fun findByRoleAndEvent(role: Role, event: String) =
            template.find<User>(Query(where("role").isEqualTo(role).and("events").inValues(event)))


    fun findOneByRoles(login: String, roles: List<Role>) =
        template.findOne<User>(Query(where("role").inValues(roles).and("_id").isEqualTo(login)))


    fun findAll() = template.findAll<User>()

    fun findOne(login: String) = template.findById<User>(login)

    fun findMany(logins: List<String>) = template.find<User>(Query(where("_id").inValues(logins)))

    fun findByLegacyId(id: Long) =
            template.findOne<User>(Query(where("legacyId").isEqualTo(id)))

    fun deleteAll() = template.remove<User>(Query())

    fun deleteOne(login: String) = template.remove<User>(Query(where("_id").isEqualTo(login)))

    fun save(user: User) = template.save(user)

    fun save(user: Mono<User>) = template.save(user)

}
