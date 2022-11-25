package mixit.mixette.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.mixette.model.MixetteDonation
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant

@Repository
class MixetteDonationRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (count().block() == 0L) {
            val usersResource = ClassPathResource("data/mixette.json")
            val donations: List<MixetteDonation> = objectMapper.readValue(usersResource.inputStream)
            donations.forEach { insert(it).block() }
            logger.info("Mixette data initialization complete")
        }
    }

    fun count() = template.count<MixetteDonation>()

    fun insert(donation: MixetteDonation) =
        template.insert(donation).doOnSuccess { _ -> logger.info("Save new Mixette donation $donation") }

    fun update(donation: MixetteDonation) =
        template.save(donation).doOnSuccess { _ -> logger.info("Update Mixette donation $donation") }

    suspend fun findAll() =
        template.findAll<MixetteDonation>().collectList().awaitSingle()

    fun findAllByYear(year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("year").isEqualTo(year)))

    suspend fun coFindAllByYear(year: String): List<MixetteDonation> =
        findAllByYear(year).collectList().awaitSingle()

    fun findByYearAfterNow(year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("year").isEqualTo(year).and("createdBy").gt(Instant.now())))

    suspend fun coFindByYearAfterNow(year: String): List<MixetteDonation> =
        findByYearAfterNow(year).collectList().awaitSingle()

    fun findOne(id: String) =
        template.findById<MixetteDonation>(id)

    suspend fun coFindOne(id: String) =
        findOne(id).awaitSingle()

    suspend fun coFindOneOrNull(id: String) =
        findOne(id).awaitSingleOrNull()

    fun deleteOne(id: String) =
        template.remove<MixetteDonation>(Query(Criteria.where("_id").isEqualTo(id)))

    fun findByTicketNumber(ticketNumber: String, year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("encryptedTicketNumber").isEqualTo(ticketNumber).and("year").isEqualTo(year)))

    suspend fun coFindByTicketNumber(ticketNumber: String, year: String): List<MixetteDonation> =
        findByTicketNumber(ticketNumber, year).collectList().awaitSingle()

    fun findByEmail(email: String, year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("encryptedUserEmail").isEqualTo(email).and("year").isEqualTo(year)))

    fun findByOrganizationLogin(login: String, year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("organizationLogin").isEqualTo(login).and("year").isEqualTo(year)))

    suspend fun coFindByOrganizationLogin(login: String, year: String): List<MixetteDonation> =
        findByOrganizationLogin(login, year).collectList().awaitSingle()
}
