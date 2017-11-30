package mixit.repository

import mixit.model.Favorite
import mixit.util.Cryptographer
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
class FavoriteRepository(private val template: ReactiveMongoTemplate, val cryptographer: Cryptographer) {

    fun findByEmail(email: String): Flux<Favorite> = template.find<Favorite>(Query(Criteria.where("email").isEqualTo(cryptographer.encrypt(email))))

    fun findByTalkAndEmail(email: String, talkId: String): Mono<Favorite> = template.findOne(Query(Criteria.where("email").isEqualTo(cryptographer.encrypt(email))
            .andOperator(Criteria.where("talkId").isEqualTo(talkId))), Favorite::class.java)

    fun save(favorite: Favorite) = template.save(favorite)

    fun delete(email: String, talkId: String) = template.remove<Favorite>(Query(Criteria.where("email").isEqualTo(cryptographer.encrypt(email))
            .andOperator(Criteria.where("talkId").isEqualTo(talkId))))
}