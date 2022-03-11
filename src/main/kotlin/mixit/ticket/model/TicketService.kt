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
) : CacheTemplate<CachedFinalTicket>() {

    override val cacheZone: CacheZone = CacheZone.TICKET

    override fun findAll(): Mono<List<CachedFinalTicket>> =
        findAll { repository.findAll().map { ticket -> CachedFinalTicket(ticket) }.collectList() }

    fun findByNumber(number: String): Mono<CachedFinalTicket> =
        findAll().flatMap { tickets -> Mono.justOrEmpty(tickets.firstOrNull { it.number == number }) }

    fun findByEncryptedEmail(encryptedEmail: String): Mono<CachedFinalTicket> =
        findAll().flatMap { tickets -> Mono.justOrEmpty(tickets.firstOrNull { it.encryptedEmail == encryptedEmail }) }

    fun findByLogin(login: String?): Mono<CachedFinalTicket> =
        findAll().flatMap { tickets ->
            val ticket = tickets.filter { it.login != null }.firstOrNull() { it.login == login }
            return@flatMap if (ticket == null) Mono.empty() else Mono.just(ticket)
        }

    fun save(ticket: FinalTicket): Mono<CachedFinalTicket> =
        findByEncryptedEmail(ticket.encryptedEmail)
            .flatMap { Mono.error<CachedFinalTicket> { DuplicateException("") } }
            .switchIfEmpty(repository.save(ticket).map { CachedFinalTicket(it) }
                .doOnSuccess { cache.invalidateAll() })

    fun deleteOne(id: String) =
        repository.deleteOne(id).doOnSuccess { cache.invalidateAll() }
}