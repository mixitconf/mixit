package mixit.mixette.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.mixette.model.MixetteDonation
import mixit.ticket.model.Ticket
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

@Repository
class MixetteDonationRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        deleteAll().block()
                if (count().block() == 0L) {
            val usersResource = ClassPathResource("data/mixette.json")
            val donations: List<MixetteDonation> = objectMapper.readValue(usersResource.inputStream)
            donations.forEach { insert(it).block() }
            logger.info("Mixette data initialization complete")
        }
    }

    fun deleteAll() =
        template.remove<Ticket>(Query())

    fun count() = template.count<MixetteDonation>()

    fun insert(donation: MixetteDonation) =
        template.insert(donation).doOnSuccess { _ -> logger.info("Save new Mixette donation $donation") }

    fun update(donation: MixetteDonation) =
        template.save(donation).doOnSuccess { _ -> logger.info("Update Mixette donation $donation") }

    suspend fun findAll() =
        template.findAll<MixetteDonation>().collectList().awaitSingle()

    suspend fun findAllByYear(year: String): List<MixetteDonation> =
        template
            .find<MixetteDonation>(Query(Criteria.where("year").isEqualTo(year)))
            .collectList()
            .awaitSingle()

    fun findByYearAfterNow(year: String): Flux<MixetteDonation> =
        template
            .find<MixetteDonation>(Query(Criteria.where("year").isEqualTo(year)))


    suspend fun findOneOrNull(id: String) =
        template.findById<MixetteDonation>(id).awaitSingleOrNull()

    fun deleteOne(id: String) =
        template.remove<MixetteDonation>(Query(Criteria.where("_id").isEqualTo(id)))

    suspend fun findByTicketNumber(ticketNumber: String, year: String): List<MixetteDonation> =
        template
            .find<MixetteDonation>(Query(Criteria.where("encryptedTicketNumber").isEqualTo(ticketNumber).and("year").isEqualTo(year)))
            .collectList()
            .awaitSingle()

    fun findByEmail(email: String, year: String): Flux<MixetteDonation> =
        template.find(Query(Criteria.where("encryptedUserEmail").isEqualTo(email).and("year").isEqualTo(year)))

    suspend fun findByOrganizationLogin(login: String, year: String): List<MixetteDonation> =
        template
            .find<MixetteDonation>(Query(Criteria.where("organizationLogin").isEqualTo(login).and("year").isEqualTo(year)))
            .collectList()
            .awaitSingle()
}
