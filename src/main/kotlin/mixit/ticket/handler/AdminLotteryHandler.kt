package mixit.ticket.handler

import mixit.MixitProperties
import mixit.security.model.Cryptographer
import mixit.ticket.model.LotteryTicket
import mixit.ticket.repository.LotteryRepository
import mixit.util.extractFormData
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import java.util.Random

@Component
class AdminLotteryHandler(
    private val ticketRepository: LotteryRepository,
    private val properties: MixitProperties,
    private val cryptographer: Cryptographer
) {

    companion object {
        const val TEMPLATE_LIST = "admin-lottery"
        const val LIST_URI = "/admin/lottery"
    }

    fun eraseRank(req: ServerRequest) = ticketRepository.eraseRank().flatMap {
        adminTicketing(req)
    }

    fun randomDraw(req: ServerRequest) =
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
            .flatMap { adminTicketing(req) }

    fun adminTicketing(req: ServerRequest): Mono<ServerResponse> {
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
        return ok().render(
            TEMPLATE_LIST,
            mapOf(
                Pair("tickets", tickets),
                Pair("title", "admin.ticketing.title")
            )
        )
    }

    fun adminDeleteTicketing(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            ticketRepository
                .deleteOne(cryptographer.encrypt(formData["email"])!!)
                .then(seeOther("${properties.baseUri}$LIST_URI"))
        }
}
