package mixit.ticket.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitProperties
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate
import mixit.security.model.Cryptographer
import mixit.ticket.model.LotteryTicket
import mixit.ticket.repository.LotteryRepository
import mixit.util.coExtractFormData
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
import reactor.kotlin.core.publisher.toFlux
import java.util.Random

@Component
class AdminLotteryHandler(
    private val ticketRepository: LotteryRepository,
    private val properties: MixitProperties,
    private val cryptographer: Cryptographer
) {

    companion object {
        const val LIST_URI = "/admin/lottery"
    }

    suspend fun eraseRank(req: ServerRequest): ServerResponse {
        ticketRepository.eraseRank()
        return adminTicketing(req)
    }

    suspend fun randomDraw(req: ServerRequest) =
        ticketRepository
            .findAll()
            .collectList()
            .flatMapMany {
                it.shuffled(Random())
                    .distinctBy { listOf(it.firstname, it.lastname) }
                    .mapIndexed { index, ticket -> ticket.copy(rank = index + 1) }
                    .toFlux()
            }
            .flatMap {
                ticketRepository.save(it)
            }
            .collectList()
            .awaitSingle()
            .let { adminTicketing(req) }

    suspend fun adminTicketing(req: ServerRequest): ServerResponse {
        val tickets = ticketRepository.findAll()
            .map {
                LotteryTicket(
                    cryptographer.decrypt(it.email)!!,
                    cryptographer.decrypt(it.firstname)!!,
                    cryptographer.decrypt(it.lastname)!!,
                    it.rank
                )
            }
            .sort(
                Comparator.comparing(LotteryTicket::lastname)
                    .thenComparing(Comparator.comparing(LotteryTicket::firstname))
            )
        return ok().renderAndAwait(
            MustacheTemplate.AdminLottery.template,
            mapOf("tickets" to tickets, TITLE to "admin.ticketing.title")
        )
    }

    suspend fun adminDeleteTicketing(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        ticketRepository.deleteOne(cryptographer.encrypt(formData["email"])!!).awaitSingle()
        return seeOther("${properties.baseUri}$LIST_URI")
    }
}
