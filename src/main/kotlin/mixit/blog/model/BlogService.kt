package mixit.blog.model

import mixit.blog.repository.PostRepository
import mixit.talk.model.Language
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.UserUpdateEvent
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class BlogService(private val repository: PostRepository, private val userService: UserService) : CacheTemplate<CachedPost>() {

    override val cacheZone: CacheZone = CacheZone.BLOG

    override fun findAll(): Flux<CachedPost> =
        findAll { repository.findAll().flatMap { post -> loadPostWriters(post) } }

    fun findBySlug(slug: String, lang: Language) =
        findAll().collectList().flatMap { elements -> Mono.justOrEmpty(elements.firstOrNull { it.slug[lang] == slug }) }

    fun save(event: Post) =
        repository.save(event).doOnSuccess { cacheList.invalidateAll() }

    @EventListener
    fun handleUserUpdate(userUpdateEvent: UserUpdateEvent) {
        findAll()
            .collectList()
            .map { blogs ->
                blogs.any { blog ->
                    blog.author.login == userUpdateEvent.user.login
                }
            }
            .block()
            .also {
                if (it != null && it) {
                    invalidateCache()
                }
            }
    }

    private fun loadPostWriters(post: Post): Mono<CachedPost> =
        userService.findOne(post.authorId)
            .map { user -> CachedPost(post, user.toUser()) }
            .switchIfEmpty { Mono.justOrEmpty(CachedPost(post, User())) }

    fun deleteOne(id: String) =
        repository.deleteOne(id).doOnSuccess { cacheList.invalidateAll() }

}