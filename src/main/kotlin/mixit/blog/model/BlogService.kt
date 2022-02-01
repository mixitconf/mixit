package mixit.blog.model

import mixit.blog.repository.PostRepository
import mixit.talk.model.Language
import mixit.user.repository.UserRepository
import mixit.util.CacheTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class BlogService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : CacheTemplate<CachedPost>() {

    override fun findAll(): Flux<CachedPost> =
        findAll { postRepository.findAll().flatMap { post -> loadPostWriters(post) } }

    fun findBySlug(slug: String, lang: Language) =
        findAll().collectList().flatMap { elements -> Mono.justOrEmpty(elements.firstOrNull { it.slug[lang] == slug }) }

    fun save(event: Post) =
        postRepository.save(event).also { cacheList.invalidateAll() }


    private fun loadPostWriters(post: Post): Mono<CachedPost> =
        userRepository.findOne(post.authorId!!).map { user -> CachedPost(post, user) }

    fun deleteOne(id: String) =
        postRepository.deleteOne(id).also { cacheList.invalidateAll() }

}