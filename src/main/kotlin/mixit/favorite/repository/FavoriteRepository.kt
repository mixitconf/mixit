package mixit.favorite.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
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
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Repository
class FavoriteRepository(
    private val template: ReactiveMongoTemplate,
    val cryptographer: Cryptographer,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val favoriteResource = ClassPathResource("data/favorite.json")
            val favorites: List<Favorite> = objectMapper.readValue(favoriteResource.inputStream)
            favorites.forEach { save(it).block(Duration.ofSeconds(10)) }
            logger.info("Favorite data initialization complete")
        }
    }

    fun count() = template.count<Favorite>()

    fun findAll() = template.findAll<Favorite>()

    fun findByEmail(email: String): Flux<Favorite> =
        template.find(Query(Criteria.where("email").isEqualTo(cryptographer.encrypt(email))))

    suspend fun coFindByEmail(email: String): List<Favorite> =
        findByEmail(email).collectList().awaitSingle()

    fun findByEmailAndTalk(email: String, talkId: String): Mono<Favorite> = template.findOne(
        Query(
            Criteria.where("email").isEqualTo(cryptographer.encrypt(email))
                .andOperator(Criteria.where("talkId").isEqualTo(talkId))
        ),
        Favorite::class.java
    )

    suspend fun coFindByEmailAndTalk(email: String, talkId: String): Favorite =
        findByEmailAndTalk(email, talkId).awaitSingle()

    fun findByEmailAndTalks(email: String, talkIds: List<String>) = template.find<Favorite>(
        Query(
            Criteria.where("email").isEqualTo(cryptographer.encrypt(email))
                .andOperator(Criteria.where("talkId").inValues(talkIds))
        )
    )

    fun save(favorite: Favorite) = template.save(favorite)

    fun delete(email: String, talkId: String) = template.remove<Favorite>(
        Query(
            Criteria.where("email").isEqualTo(cryptographer.encrypt(email))
                .andOperator(Criteria.where("talkId").isEqualTo(talkId))
        )
    )

    fun deleteAll() = template.remove<Favorite>(Query())
}
