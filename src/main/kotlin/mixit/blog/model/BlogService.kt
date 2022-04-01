package mixit.blog.model

import mixit.blog.repository.PostRepository
import mixit.talk.model.Language
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.util.cache.CacheTemplate
import mixit.util.cache.CacheZone
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class BlogService(private val repository: PostRepository, private val userService: UserService) : CacheTemplate<CachedPost>() {

    override val cacheZone: CacheZone = CacheZone.BLOG

    override fun findAll(): Mono<List<CachedPost>> =
        findAll { repository.findAll().flatMap { post -> loadPostWriters(post) }.collectList() }

    fun findBySlug(slug: String, lang: Language) =
        findAll().flatMap { elements -> Mono.justOrEmpty(elements.firstOrNull { it.slug[lang] == slug }) }

    fun save(event: Post) =
        repository.save(event).doOnSuccess { cache.invalidateAll() }

    private fun loadPostWriters(post: Post): Mono<CachedPost> =
        userService.findOne(post.authorId)
            .map { user -> CachedPost(post, user.toUser()) }
            .switchIfEmpty { Mono.justOrEmpty(CachedPost(post, User())) }

    fun deleteOne(id: String) =
        repository.deleteOne(id).doOnSuccess { cache.invalidateAll() }
}
