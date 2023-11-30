package mixit.favorite.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.favorite.model.Favorite
import mixit.security.model.Cryptographer
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class FavoriteRepository(
    private val template: ReactiveMongoTemplate,
    private val cryptographer: Cryptographer,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            ClassPathResource("data/favorite.json").inputStream.use { resource ->
                val favorites: List<Favorite> = objectMapper.readValue(resource)
                favorites.forEach { save(it).block(Duration.ofSeconds(10)) }
                logger.info("Favorite data initialization complete")
            }
        }
    }

    fun count() = template.count<Favorite>()

    suspend fun findAll(): List<Favorite> =
        template.findAll<Favorite>().collectList().awaitSingle()

    suspend fun findByEmail(email: String): List<Favorite> =
        template
            .find<Favorite>(Query(Criteria.where("email").isEqualTo(cryptographer.encrypt(email))))
            .collectList()
            .awaitSingle()

    suspend fun findByEmailAndTalk(email: String, talkId: String): Favorite? = template.findOne(
        Query(
            Criteria.where("email").isEqualTo(cryptographer.encrypt(email))
                .andOperator(Criteria.where("talkId").isEqualTo(talkId))
        ),
        Favorite::class.java
    ).awaitSingleOrNull()

    fun save(favorite: Favorite) = template.save(favorite)

    fun delete(email: String, talkId: String) = template.remove<Favorite>(
        Query(
            Criteria.where("email").isEqualTo(cryptographer.encrypt(email))
                .andOperator(Criteria.where("talkId").isEqualTo(talkId))
        )
    )

    fun deleteAll() = template.remove<Favorite>(Query())
}
