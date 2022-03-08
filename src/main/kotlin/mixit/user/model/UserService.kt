package mixit.user.model

import mixit.user.repository.UserRepository
import mixit.util.cache.CacheTemplate
import mixit.util.cache.CacheZone
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

data class UserUpdateEvent(val user: User): ApplicationEvent(user)

@Service
class UserService(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : CacheTemplate<CachedUser>() {

    override val cacheZone: CacheZone = CacheZone.USER

    override fun findAll(): Mono<List<CachedUser>> =
        findAll { userRepository.findAll().map { user -> CachedUser(user) }.collectList() }

    fun findOneByEncryptedEmail(email: String): Mono<CachedUser> =
        findAll().flatMap { elements -> Mono.justOrEmpty(elements.firstOrNull { it.email == email }) }

    fun findByRoles(vararg roles: Role): Mono<List<CachedUser>> =
        findAll().map { elements -> elements.filter { roles.contains(it.role) } }

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
}