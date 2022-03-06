package mixit.ticket.model

import mixit.security.model.Cryptographer
import mixit.ticket.repository.FinalTicketRepository
import mixit.user.model.UserUpdateEvent
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import mixit.util.errors.DuplicateException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class TicketService(
    private val repository: FinalTicketRepository,
    private val cryptographer: Cryptographer
) : CacheTemplate<CachedFinalTicket>() {

    override val cacheZone: CacheZone = CacheZone.TICKET

    override fun findAll(): Flux<CachedFinalTicket> =
        findAll { repository.findAll().map { ticket -> CachedFinalTicket(ticket) } }

    fun findByNumber(number: String): Mono<CachedFinalTicket> =
        findAll().collectList().flatMap { tickets -> Mono.justOrEmpty(tickets.firstOrNull { it.number == number }) }

    fun findByEmail(email: String): Mono<CachedFinalTicket> =
        findAll().collectList().flatMap { tickets -> Mono.justOrEmpty(tickets.firstOrNull { it.email == email }) }

    fun findByLogin(login: String?): Mono<CachedFinalTicket> =
        findAll().collectList().flatMap { tickets ->
            val ticket = tickets.filter{it.login !=null}.firstOrNull() { it.login == login }
            return@flatMap if (ticket == null) Mono.empty() else Mono.just(ticket)
        }

    fun save(ticket: FinalTicket): Mono<CachedFinalTicket> =
        findByEmail(ticket.email)
            .flatMap { Mono.error<CachedFinalTicket> { DuplicateException("")} }
            .switchIfEmpty(repository.save(ticket).map { CachedFinalTicket(it) }.doOnSuccess { cacheList.invalidateAll() })

    @EventListener
    fun handleUserUpdate(userUpdateEvent: UserUpdateEvent) {
        val user = userUpdateEvent.user
        val email = cryptographer.decrypt(user.email)!!
        findByEmail(email)
            .map {
                repository.save(it.copy(firstname = it.firstname, lastname = it.lastname).toEntity())
                    .also { invalidateCache() }

            }
            .switchIfEmpty (
                findByLogin(user.login)
                    .map {
                        repository
                            .save(it.copy(email = email, firstname = it.firstname, lastname = it.lastname).toEntity())
                            .also { invalidateCache() }
                    }
            )
            .block()
        }

    fun deleteOne(id: String) =
        repository.deleteOne(id).doOnSuccess { cacheList.invalidateAll() }
}