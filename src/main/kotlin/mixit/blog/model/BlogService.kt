package mixit.blog.model

import mixit.blog.repository.PostRepository
import mixit.talk.model.Language
import mixit.user.model.User
import mixit.user.repository.UserRepository
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class BlogService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : CacheTemplate<CachedPost>() {

    override val cacheZone: CacheZone = CacheZone.BLOG

    override fun findAll(): Flux<CachedPost> =
        findAll { postRepository.findAll().flatMap { post -> loadPostWriters(post) } }

    fun findBySlug(slug: String, lang: Language) =
        findAll().collectList().flatMap { elements -> Mono.justOrEmpty(elements.firstOrNull { it.slug[lang] == slug }) }

    fun save(event: Post) =
        postRepository.save(event).doOnSuccess { cacheList.invalidateAll() }


    private fun loadPostWriters(post: Post): Mono<CachedPost> =
        userRepository.findOne(post.authorId!!)
            .map { user -> CachedPost(post, user) }
            .switchIfEmpty { Mono.justOrEmpty(CachedPost(post, User())) }

    fun deleteOne(id: String) =
        postRepository.deleteOne(id).doOnSuccess { cacheList.invalidateAll() }

}