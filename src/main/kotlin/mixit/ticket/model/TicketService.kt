package mixit.ticket.model

import mixit.ticket.repository.FinalTicketRepository
import mixit.util.cache.CacheTemplate
import mixit.util.cache.CacheZone
import mixit.util.errors.DuplicateException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TicketService(
    private val repository: FinalTicketRepository
) : CacheTemplate<CachedTicket>() {

    override val cacheZone: CacheZone = CacheZone.TICKET

    override fun findAll(): Mono<List<CachedTicket>> =
        findAll { repository.findAll().map { ticket -> CachedTicket(ticket) }.collectList() }

    fun findByNumber(number: String): Mono<CachedTicket> =
        findAll().flatMap { tickets -> Mono.justOrEmpty(tickets.firstOrNull { it.number == number }) }

    fun findByEncryptedEmail(encryptedEmail: String): Mono<CachedTicket> =
        findAll().flatMap { tickets -> Mono.justOrEmpty(tickets.firstOrNull { it.encryptedEmail == encryptedEmail }) }

    fun findByLogin(login: String?): Mono<CachedTicket> =
        findAll().flatMap { tickets ->
            val ticket = tickets.filter { it.login != null }.firstOrNull() { it.login == login }
            return@flatMap if (ticket == null) Mono.empty() else Mono.just(ticket)
        }

    fun save(ticket: Ticket): Mono<CachedTicket> =
        findByEncryptedEmail(ticket.encryptedEmail)
            .flatMap { Mono.error<CachedTicket> { DuplicateException("") } }
            .switchIfEmpty(
                repository.save(ticket).map { CachedTicket(it) }
                    .doOnSuccess { cache.invalidateAll() }
            )

    fun deleteOne(id: String) =
        repository.deleteOne(id).doOnSuccess { cache.invalidateAll() }
}
