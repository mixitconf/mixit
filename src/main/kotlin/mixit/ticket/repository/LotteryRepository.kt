package mixit.ticket.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.ticket.model.LotteryTicket
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.data.mongodb.core.updateMulti
import org.springframework.stereotype.Repository

@Repository
class LotteryRepository(
    private val template: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        deleteAll().block()
        if (count().block() == 0L) {
            ClassPathResource("data/lottery.json").inputStream.use { resource ->
                val tickets: List<LotteryTicket> = objectMapper.readValue(resource)
                tickets.forEach { save(it).block() }
                logger.info("Lottery data initialization complete")
            }
        }
    }

    fun count() = template.count<LotteryTicket>()

    fun save(ticket: LotteryTicket) =
        template.save(ticket).doOnSuccess { _ -> logger.info("Save new lottery ticket $ticket") }

    suspend fun findAll() = template.findAll<LotteryTicket>().doOnComplete { logger.info("Load all lottery tickets") }.collectList().awaitSingle()

    suspend fun eraseRank() = template.updateMulti<LotteryTicket>(Query(), Update().set("rank", null)).awaitSingle()

    fun deleteAll() = template.remove<LotteryTicket>(Query())

    fun deleteOne(id: String) = template.remove<LotteryTicket>(Query(Criteria.where("_id").isEqualTo(id)))

    suspend fun findByEncryptedEmail(email: String) =
        template.findOne<LotteryTicket>(Query(Criteria.where("email").isEqualTo(email))).awaitSingleOrNull()
}
