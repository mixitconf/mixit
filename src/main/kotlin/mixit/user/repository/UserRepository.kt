package mixit.user.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.security.model.Cryptographer
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.desanonymize
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
import reactor.core.publisher.Mono

@Repository
class UserRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper,
    private val cryptographer: Cryptographer
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            ClassPathResource("data/users.json").inputStream.use { resource ->
                val users: List<User> = objectMapper.readValue(resource)
                users
                    .map { it.desanonymize(cryptographer) }
                    .forEach { save(it).block() }
                logger.info("Users data initialization complete")
            }
        }
    }

    suspend fun findFullText(criteria: List<String>): List<User> {
        val textCriteria = TextCriteria()
        criteria.forEach { textCriteria.matching(it) }

        val query = TextQuery(textCriteria).sortByScore()
        return template.find<User>(query).collectList().awaitSingle()
    }

    fun count() = template.count<User>()

    suspend fun findByNonEncryptedEmail(email: String): User? =
        template
            .findOne<User>(
                Query(
                    where("role").inValues(Role.STAFF, Role.STAFF_IN_PAUSE, Role.USER, Role.VOLUNTEER)
                        .orOperator(
                            where("email").isEqualTo(cryptographer.encrypt(email)),
                            where("emailHash").isEqualTo(email.encodeToMd5())
                        )
                )
            )
            .awaitSingleOrNull()

    suspend fun findByRoles(roles: List<Role>): List<User> =
        template.find<User>(Query(where("role").inValues(roles))).collectList().awaitSingle()

    suspend fun findOneByRoles(login: String, roles: List<Role>): User? =
        template
            .findOne<User>(Query(where("role").inValues(roles).and("_id").isEqualTo(login)))
            .awaitSingleOrNull()

    suspend fun findAll(): List<User> =
        template
            .findAll<User>().doOnComplete { logger.info("Load all users") }
            .collectList()
            .awaitSingle()

    suspend fun findAllByIds(login: List<String>): List<User> {
        val criteria = where("login").inValues(login)
        return template.find<User>(Query(criteria)).collectList().awaitSingle()
    }

    suspend fun findOneOrNull(login: String): User? =
        template.findById<User>(login).awaitSingleOrNull()

    suspend fun findByLegacyId(id: Long): User? =
        template.findOne<User>(Query(where("legacyId").isEqualTo(id))).awaitSingleOrNull()

    fun deleteOne(login: String) =
        template.remove<User>(Query(where("_id").isEqualTo(login)))

    fun save(user: User) =
        template.save(user)

    fun save(user: Mono<User>) =
        template.save(user)
}
