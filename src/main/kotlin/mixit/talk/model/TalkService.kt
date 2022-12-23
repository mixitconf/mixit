package mixit.talk.model

import kotlinx.coroutines.runBlocking
import mixit.talk.repository.TalkRepository
import mixit.user.model.UserUpdateEvent
import mixit.user.repository.UserRepository
import mixit.util.cache.CacheCaffeineTemplate
import mixit.util.cache.CacheZone
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TalkService(
    private val talkRepository: TalkRepository,
    private val userRepository: UserRepository
) : CacheCaffeineTemplate<CachedTalk>() {

    override val cacheZone: CacheZone = CacheZone.TALK

    override fun loader(): suspend () -> List<CachedTalk> =
        { talkRepository.findAll().map { talk -> loadSpeakers(talk) } }

    fun save(talk: Talk): Mono<Talk> =
        talkRepository.save(talk).doOnSuccess { cache.invalidateAll() }

    suspend fun findByEvent(eventId: String, topic: String? = null): List<CachedTalk> =
        findAll()
            .filter { it.event == eventId }
            .filter { if (topic == null) true else it.topic == topic }
            .sortedBy { it.start }

    suspend fun findBySlug(slug: String) =
        findAll().first { it.slug == slug }

    suspend fun findByEventAndSlug(eventId: String, slug: String) =
        findAll().first { it.slug.endsWith(slug) && it.event == eventId }

    suspend fun findBySpeakerId(speakerIds: List<String>, talkIdExcluded: String): List<CachedTalk> =
        findAll().filter { talk ->
            val speakers = talk.speakers.map { it.login }
            speakers.any { speakerIds.contains(it) } && talk.id != talkIdExcluded
        }

    suspend fun findByEventAndTalkIds(eventId: String, talkIds: List<String>, topic: String? = null): List<CachedTalk> =
        findAll()
            .filter { it.event == eventId && talkIds.contains(it.id) }
            .filter { if (topic == null) true else it.topic == topic }
            .sortedBy { it.start }

    @EventListener(UserUpdateEvent::class)
    fun handleUserUpdate(userUpdateEvent: UserUpdateEvent) =
        runBlocking {
            findAll()
                .any { talk -> talk.speakers.map { it.login }.contains(userUpdateEvent.user.login) }
                .also {
                    if (it) {
                        invalidateCache()
                    }
                }
        }

    private suspend fun loadSpeakers(talk: Talk): CachedTalk {
        val speakers = userRepository.findAllByIds(talk.speakerIds)
        return CachedTalk(talk, speakers)
    }

    fun deleteOne(id: String) =
        talkRepository.deleteOne(id).doOnSuccess { cache.invalidateAll() }
}
