package mixit.blog.model

import mixit.blog.repository.PostRepository
import mixit.talk.model.Language
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.util.cache.CacheCaffeineTemplate
import mixit.util.cache.CacheZone
import org.springframework.stereotype.Service

@Service
class BlogService(private val repository: PostRepository, private val userService: UserService) :
    CacheCaffeineTemplate<CachedPost>() {

    override val cacheZone: CacheZone = CacheZone.BLOG

    override fun loader(): suspend () -> List<CachedPost> =
        { repository.findAll().map { post -> loadPostWriters(post) } }

    suspend fun findBySlug(slug: String): CachedPost? =
        findAll().firstOrNull { it.slug[Language.ENGLISH] == slug || it.slug[Language.FRENCH] == slug }

    fun save(event: Post) =
        repository.save(event).doOnSuccess { cache.invalidateAll() }

    private suspend fun loadPostWriters(post: Post): CachedPost =
        userService.findOneOrNull(post.authorId)
            ?.let { CachedPost(post, it.toUser()) }
            ?: CachedPost(post, User())

    fun deleteOne(id: String) =
        repository.deleteOne(id).doOnSuccess { cache.invalidateAll() }
}
