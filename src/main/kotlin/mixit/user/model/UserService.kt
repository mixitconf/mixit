package mixit.user.model

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.security.model.Cryptographer
import mixit.user.repository.UserRepository
import mixit.util.cache.CacheTemplate
import mixit.util.cache.CacheZone
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

data class UserUpdateEvent(val user: User) : ApplicationEvent(user)

@Service
class UserService(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val cryptographer: Cryptographer
) : CacheTemplate<CachedUser>() {

    override val cacheZone: CacheZone = CacheZone.USER

    override fun findAll(): Mono<List<CachedUser>> =
        findAll { userRepository.findAll().map { user -> CachedUser(user) }.collectList() }

    fun findOneByEncryptedEmail(email: String): Mono<CachedUser> =
        findAll().flatMap { elements -> Mono.justOrEmpty(elements.firstOrNull { it.email == email }) }

    suspend fun coFindOneByEncryptedEmail(email: String): CachedUser? =
        findOneByEncryptedEmail(email).awaitSingleOrNull()

    fun findOneByNonEncryptedEmail(email: String): Mono<CachedUser> =
        cryptographer.encrypt(email)!!.let { encryptedEmail ->
            findAll().flatMap { elements -> Mono.justOrEmpty(elements.firstOrNull { it.email == encryptedEmail }) }
        }

    fun findByRoles(vararg roles: Role): Mono<List<CachedUser>> =
        findAll().map { elements -> elements.filter { roles.contains(it.role) } }

    suspend fun coFindByRoles(staff: Role, staffInPause: Role) =
        findByRoles(staff, staffInPause).awaitSingle()

    fun findAllByIds(userIds: List<String>): Mono<List<CachedUser>> =
        findAll().map { elements -> elements.filter { userIds.contains(it.login) } }

    fun save(user: User): Mono<User> =
        userRepository.save(user).doOnSuccess {
            cache.invalidateAll()
            eventPublisher.publishEvent(UserUpdateEvent(user))
        }

    fun deleteOne(id: String) =
        userRepository.findOne(id).flatMap { user ->
            userRepository.deleteOne(id).map {
                cache.invalidateAll()
                eventPublisher.publishEvent(UserUpdateEvent(user))
                user
            }
        }

    fun updateReference(user: User): Mono<User> =
        findOne(user.login)
            .map {
                it.email = it.email ?: user.email
                it.newsletterSubscriber = user.newsletterSubscriber
                user
            }
}
