package mixit.user.model

import mixit.user.repository.UserRepository
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserService(private val userRepository: UserRepository) : CacheTemplate<CachedUser>() {

    override val cacheZone: CacheZone = CacheZone.TALK

    override fun findAll(): Flux<CachedUser> =
        findAll { userRepository.findAll().map { user -> CachedUser(user) } }


    fun findByRoles(vararg roles: Role): Mono<List<CachedUser>> =
        findAll().collectList().map { elements -> elements.filter { roles.contains(it.role) } }

    fun findAllByIds(userIds: List<String>): Mono<List<CachedUser>> =
        findAll().collectList().map { elements -> elements.filter { userIds.contains(it.login) } }

    fun save(user: User) =
        userRepository.save(user).doOnSuccess { cacheList.invalidateAll() }

    fun deleteOne(id: String) =
        userRepository.deleteOne(id).doOnSuccess { cacheList.invalidateAll() }

}