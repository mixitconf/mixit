package mixit.ticket.model

import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.security.model.Cryptographer
import mixit.ticket.repository.TicketRepository
import mixit.util.cache.CacheTemplate
import mixit.util.cache.CacheZone
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TicketService(
    private val repository: TicketRepository,
    private val cryptographer: Cryptographer
) : CacheTemplate<CachedTicket>() {

    override val cacheZone: CacheZone = CacheZone.TICKET

    override fun findAll(): Mono<List<CachedTicket>> =
        findAll { repository.findAll().map { ticket -> CachedTicket(cryptographer, ticket) }.collectList() }

    fun findByNumber(number: String): Mono<CachedTicket> =
        findAll().flatMap { tickets -> Mono.justOrEmpty(tickets.firstOrNull { it.number == number }) }

    suspend fun coFindByNumber(number: String): CachedTicket? =
        findByNumber(number).awaitSingleOrNull()

    fun findByEmail(email: String): Mono<CachedTicket> =
        findAll().flatMap { tickets -> Mono.justOrEmpty(tickets.firstOrNull { it.email == email }) }

    suspend fun coFindByEmail(email: String): CachedTicket? =
        findByEmail(email).awaitSingleOrNull()

    fun findByLogin(login: String?): Mono<CachedTicket> =
        findAll().flatMap { tickets ->
            val ticket = tickets.filter { it.login != null }.firstOrNull() { it.login == login }
            return@flatMap if (ticket == null) Mono.empty() else Mono.just(ticket)
        }

    fun save(ticket: Ticket): Mono<CachedTicket> =
        repository
            .save(ticket)
            .map { CachedTicket(cryptographer, it) }
            .doOnSuccess { cache.invalidateAll() }

    fun deleteOne(id: String) =
        repository.deleteOne(id).doOnSuccess { cache.invalidateAll() }
}
