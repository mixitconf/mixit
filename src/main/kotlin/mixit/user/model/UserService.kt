package mixit.user.model

import mixit.user.repository.UserRepository
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class UserService(private val userRepository: UserRepository) : CacheTemplate<CachedUser>() {

    override val cacheZone: CacheZone = CacheZone.TALK

    override fun findAll(): Flux<CachedUser> =
        findAll { userRepository.findAll().map { user -> CachedUser(user) } }

    fun save(user: User) =
        userRepository.save(user).also { cacheList.invalidateAll() }

    fun deleteOne(id: String) =
        userRepository.deleteOne(id).also { cacheList.invalidateAll() }

}