package mixit.repository

import mixit.model.MixetteDonation
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
class MixetteDonationRepository(private val template: ReactiveMongoTemplate) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun insert(donation: MixetteDonation) =
        template.insert(donation).doOnSuccess { _ -> logger.info("Save new Mixette donation $donation") }

    fun update(donation: MixetteDonation) =
        template.save(donation).doOnSuccess { _ -> logger.info("Update Mixette donation $donation") }

    fun findAll() =
        template.findAll<MixetteDonation>()


    fun findAllByYear(year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("year").isEqualTo(year)))

    fun findOne(id: String) =
        template.findById<MixetteDonation>(id)

    fun deleteOne(id: String) =
        template.remove<MixetteDonation>(Query(Criteria.where("_id").isEqualTo(id)))

    fun findByLogin(login: String, year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("userLogin").isEqualTo(login).and("year").isEqualTo(year)))

    fun findByOrganizationLogin(login: String, year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("organizationLogin").isEqualTo(login).and("year").isEqualTo(year)))
}
