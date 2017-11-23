package mixit.repository

import mixit.model.Favorite
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class FavoriteRepository(private val template: ReactiveMongoTemplate) {

    fun findByUser(login: String): Flux<Favorite> = template.find<Favorite>(Query(Criteria.where("login").isEqualTo(login)))

    fun findByTalkAndUser(login: String, talkId: String): Flux<Favorite> = template.find<Favorite>(Query(Criteria.where("login").isEqualTo(login)
            .andOperator(Criteria.where("talkId").isEqualTo(talkId))))

    fun save(favorite: Favorite) = template.save(favorite)

    fun delete(login: String, talkId: String) = template.remove<Favorite>(Query(Criteria.where("login").isEqualTo(login)
            .andOperator(Criteria.where("talkId").isEqualTo(talkId))))
}