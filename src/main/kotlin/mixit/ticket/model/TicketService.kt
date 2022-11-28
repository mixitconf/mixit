package mixit.ticket.model

import kotlinx.coroutines.reactor.awaitSingle
import mixit.security.model.Cryptographer
import mixit.ticket.repository.TicketRepository
import mixit.util.cache.CacheCaffeineTemplate
import mixit.util.cache.CacheZone
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TicketService(
    private val repository: TicketRepository,
    private val cryptographer: Cryptographer
) : CacheCaffeineTemplate<CachedTicket>() {

    override val cacheZone: CacheZone = CacheZone.TICKET
    override fun loader(): suspend () -> List<CachedTicket> =
        { repository.findAll().map { ticket -> CachedTicket(cryptographer, ticket) }.collectList().awaitSingle() }

    suspend fun findByNumber(number: String): CachedTicket? =
        findAll().firstOrNull { it.number == number }

    suspend fun findByEmail(email: String): CachedTicket? =
        findAll().firstOrNull { it.email == email }

    suspend fun findByLogin(login: String?): CachedTicket? =
        findAll()
            .filter { it.login != null }
            .firstOrNull() { it.login == login }

    fun save(ticket: Ticket): Mono<CachedTicket> =
        repository
            .save(ticket)
            .map { CachedTicket(cryptographer, it) }
            .doOnSuccess { cache.invalidateAll() }

    fun deleteOne(id: String) =
        repository.deleteOne(id).doOnSuccess { cache.invalidateAll() }
}
