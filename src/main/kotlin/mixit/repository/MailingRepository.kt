package mixit.repository

import mixit.model.Mailing
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository

@Repository
class MailingRepository(private val template: ReactiveMongoTemplate) {

    fun findOne(id: String) = template.findById<Mailing>(id)

    fun findAll() = template.find<Mailing>(Query().with(Sort.by("addedAt")))

    fun deleteOne(id: String) = template.remove<Mailing>(Query(where("_id").isEqualTo(id)))

    fun save(article: Mailing) = template.save(article)
}
