package mixit.user.model

import mixit.event.model.CachedEvent
import mixit.event.model.SponsorshipLevel.GOLD
import mixit.event.model.SponsorshipLevel.LANYARD
import mixit.security.model.Cryptographer
import mixit.user.handler.dto.toSponsorDto
import mixit.user.repository.UserRepository
import mixit.util.cache.CacheCaffeineTemplate
import mixit.util.cache.CacheZone
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

data class UserUpdateEvent(val user: User) : ApplicationEvent(user)

@Service
class UserService(
    private val userRepository: UserRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val cryptographer: Cryptographer
) : CacheCaffeineTemplate<CachedUser>() {

    override val cacheZone: CacheZone = CacheZone.USER
    override fun loader(): suspend () -> List<CachedUser> {
        return { userRepository.findAll().map { user -> CachedUser(user) } }
    }

    suspend fun findOneByEncryptedEmailOrNull(email: String): CachedUser? =
        findAll().firstOrNull { it.email == email }

    suspend fun findOneByNonEncryptedEmailOrNull(email: String): CachedUser? =
        cryptographer.encrypt(email)?.let { encryptedEmail ->
            findAll().firstOrNull { it.email == encryptedEmail }
        }

    suspend fun findByRoles(vararg roles: Role): List<CachedUser> =
        findAll().filter { roles.contains(it.role) }

    suspend fun findAllByIds(userIds: List<String>): List<CachedUser> =
        findAll().filter { userIds.contains(it.login) }

    fun save(user: User): Mono<User> =
        userRepository.save(user).doOnSuccess {
            cache.invalidateAll()
            eventPublisher.publishEvent(UserUpdateEvent(user))
        }

    suspend fun deleteOne(id: String): Mono<User> =
        userRepository.findOneOrNull(id)
            ?.let { user ->
                userRepository.deleteOne(id).map {
                    cache.invalidateAll()
                    eventPublisher.publishEvent(UserUpdateEvent(user))
                    user
                }
            }
            ?: throw ChangeSetPersister.NotFoundException()

    suspend fun updateReference(user: User): User? =
        findOneOrNull(user.login)
            ?.let {
                it.email = it.email ?: user.email
                it.newsletterSubscriber = user.newsletterSubscriber
                user
            }

    fun loadSponsors(event: CachedEvent): Map<String, Any> {
        val sponsors = event.filterBySponsorLevel(GOLD) + event.filterBySponsorLevel(LANYARD)
        return mapOf(
            "sponsors-gold" to sponsors.map { it.toSponsorDto() },
            "sponsors-others" to event.sponsors.filterNot { sponsors.contains(it) }.map { it.toSponsorDto() }.distinct()
        )
    }
}
