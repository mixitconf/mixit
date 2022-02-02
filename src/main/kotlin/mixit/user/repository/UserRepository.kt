package mixit.user.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.security.model.Cryptographer
import mixit.user.model.Role
import mixit.user.model.User
import mixit.util.encodeToMd5
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.TextCriteria
import org.springframework.data.mongodb.core.query.TextQuery
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class UserRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper,
    private val cryptographer: Cryptographer
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        // We delete all users to recreate them since next MEP
        deleteAll()
        if (count().block() == 0L) {
            val usersResource = ClassPathResource("data/users.json")
            val users: List<User> = objectMapper.readValue(usersResource.inputStream)
            users.forEach { save(it).block() }
            logger.info("Users data initialization complete")
        }
    }

    fun findFullText(criteria: List<String>): Flux<User> {
        val textCriteria = TextCriteria()
        criteria.forEach { textCriteria.matching(it) }

        val query = TextQuery(textCriteria).sortByScore()
        return template.find(query)
    }

    fun count() = template.count<User>()

    fun findByYear(year: Int) =
        template.find<User>(Query(where("year").isEqualTo(year)))

    fun findByNonEncryptedEmail(email: String) = template.findOne<User>(
        Query(
            where("role").inValues(Role.STAFF, Role.STAFF_IN_PAUSE, Role.USER, Role.VOLUNTEER)
                .orOperator(where("email").isEqualTo(cryptographer.encrypt(email)), where("emailHash").isEqualTo(email.encodeToMd5()))
        )
    )

    fun findByName(firstname: String, lastname: String) =
        template.find<User>(Query(where("firstname").isEqualTo(firstname).and("lastname").isEqualTo(lastname)))

    fun findByRoles(roles: List<Role>) =
        template.find<User>(Query(where("role").inValues(roles)))

    fun findByRoleAndEvent(role: Role, event: String) =
        template.find<User>(Query(where("role").isEqualTo(role).and("events").inValues(event)))

    fun findOneByRoles(login: String, roles: List<Role>) =
        template.findOne<User>(Query(where("role").inValues(roles).and("_id").isEqualTo(login)))

    fun findAll() =
        template.findAll<User>().doOnComplete { logger.info("Load all talks")  }

    fun findAllByIds(login: List<String>): Flux<User> {
        val criteria = where("login").inValues(login)
        return template.find(Query(criteria))
    }

    fun findAllByRole(role: Role): Flux<User> {
        val criteria = where("role").isEqualTo(role)
        return template.find(Query(criteria))
    }

    fun findOne(login: String) =
        template.findById<User>(login)

    fun findMany(logins: List<String>) =
        template.find<User>(Query(where("_id").inValues(logins)))

    fun findByLegacyId(id: Long) =
        template.findOne<User>(Query(where("legacyId").isEqualTo(id)))

    fun deleteAll() =
        template.remove<User>(Query())

    fun deleteOne(login: String) =
        template.remove<User>(Query(where("_id").isEqualTo(login)))

    fun save(user: User) =
        template.save(user)

    fun save(user: Mono<User>) =
        template.save(user)
}
