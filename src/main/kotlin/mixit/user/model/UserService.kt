package mixit.user.model

import mixit.user.repository.UserRepository
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class UserUpdateEvent(val user: User): ApplicationEvent(user)

@Service
class UserService(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) : CacheTemplate<CachedUser>() {

    override val cacheZone: CacheZone = CacheZone.TALK

    override fun findAll(): Flux<CachedUser> =
        findAll { userRepository.findAll().map { user -> CachedUser(user) } }


    fun findByRoles(vararg roles: Role): Mono<List<CachedUser>> =
        findAll().collectList().map { elements -> elements.filter { roles.contains(it.role) } }

    fun findAllByIds(userIds: List<String>): Mono<List<CachedUser>> =
        findAll().collectList().map { elements -> elements.filter { userIds.contains(it.login) } }

    fun save(user: User): Mono<User> =
        userRepository.save(user).map {
            cacheList.invalidateAll()
            eventPublisher.publishEvent(UserUpdateEvent(user))
            user
        }

    fun deleteOne(id: String) =
        userRepository.findOne(id).flatMap { user ->
            userRepository.deleteOne(id).map {
                cacheList.invalidateAll()
                eventPublisher.publishEvent(UserUpdateEvent(user))
                user
            }
        }
}