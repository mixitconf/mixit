package mixit.ticket.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitProperties
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate.AdminLottery
import mixit.security.model.Cryptographer
import mixit.ticket.model.LotteryTicket
import mixit.ticket.repository.LotteryRepository
import mixit.util.extractFormData
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
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

    suspend fun randomDraw(req: ServerRequest): ServerResponse {
        val tickets = ticketRepository.findAll()
        tickets.shuffled(Random())
            .distinctBy { listOf(it.firstname, it.lastname) }
            .mapIndexed { index, ticket -> ticket.copy(rank = index + 1) }
            .onEach { ticketRepository.save(it).awaitSingle() }

        return adminTicketing(req)
    }

    suspend fun adminTicketing(req: ServerRequest): ServerResponse {
        val tickets = ticketRepository.findAll()
            .map {
                LotteryTicket(
                    cryptographer.decrypt(it.email)!!,
                    cryptographer.decrypt(it.firstname)!!,
                    cryptographer.decrypt(it.lastname)!!,
                    it.rank,
                    it.interests
                )
            }
            .sortedBy { it.lastname }
        return ok().renderAndAwait(
            AdminLottery.template,
            mapOf("tickets" to tickets, TITLE to AdminLottery.title)
        )
    }

    suspend fun adminDeleteTicketing(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        ticketRepository.deleteOne(cryptographer.encrypt(formData["email"])!!).awaitSingle()
        return seeOther("${properties.baseUri}$LIST_URI")
    }
}
