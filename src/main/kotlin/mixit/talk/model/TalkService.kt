package mixit.talk.model

import mixit.talk.repository.TalkRepository
import mixit.user.repository.UserRepository
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class TalkService(
    private val talkRepository: TalkRepository,
    private val userRepository: UserRepository
) : CacheTemplate<CachedTalk>() {

    override val cacheZone: CacheZone = CacheZone.TALK

    override fun findAll(): Flux<CachedTalk> =
        findAll { talkRepository.findAll().flatMap { talk -> loadSpeakers(talk) } }

    fun save(talk: Talk) =
        talkRepository.save(talk).also { cacheList.invalidateAll() }

    fun findByEvent(eventId: String, topic: String? = null): Mono<List<CachedTalk>> =
        findAll().collectList().flatMap { talks ->
            val eventTalks = talks.filter { it.event == eventId }.sortedBy { it.start }
            Mono.justOrEmpty(if (topic == null) eventTalks else eventTalks.filter { it.topic == topic })
        }

    fun findBySlug(slug: String) =
        findAll().collectList().flatMap { talks ->
            Mono.justOrEmpty(talks.first { it.slug == slug })
        }

    fun findByEventAndSlug(eventId: String, slug: String) =
        findAll().collectList().flatMap { talks ->
            Mono.justOrEmpty(talks.first { it.slug == slug && it.event == eventId})
        }

    fun findBySpeakerId(speakerIds: List<String>, talkIdExcluded: String): Mono<List<CachedTalk>> =
        findAll().collectList().flatMap { talks ->
            Mono.justOrEmpty(talks.filter { talk ->
                val speakers = talk.speakers.map { it.login }
                speakers.any { speakerIds.contains(it) } && talk.id != talkIdExcluded
            })
        }

    fun findByEventAndTalkIds(eventId: String, talkIds: List<String>, topic: String? = null): Mono<List<CachedTalk>> =
        findAll().collectList().flatMap { talks ->
            val eventTalks = talks.filter { it.event == eventId && talkIds.contains(it.id) }.sortedBy { it.start }
            Mono.justOrEmpty(if (topic == null) eventTalks else eventTalks.filter { it.topic == topic })
        }

    private fun loadSpeakers(talk: Talk): Mono<CachedTalk> =
        userRepository.findAllByIds(talk.speakerIds).collectList()
            .map { speakers ->
                CachedTalk(talk, speakers)
            }
            .switchIfEmpty { Mono.justOrEmpty(CachedTalk(talk, emptyList())) }

    fun deleteOne(id: String) =
        talkRepository.deleteOne(id).also { cacheList.invalidateAll() }

}