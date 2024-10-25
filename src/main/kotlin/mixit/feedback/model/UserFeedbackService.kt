package mixit.feedback.model

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.feedback.repository.UserFeedbackRepository
import mixit.talk.repository.TalkRepository
import mixit.user.repository.UserRepository
import mixit.util.cache.CacheCaffeineUnitaryTemplate
import mixit.util.cache.CacheZone
import mixit.util.errors.NotFoundException
import org.springframework.stereotype.Service

@Service
class UserFeedbackService(
    private val userFeedbackRepository: UserFeedbackRepository,
    private val userRepository: UserRepository,
    private val talkRepository: TalkRepository
) : CacheCaffeineUnitaryTemplate<CachedUserFeedback>() {

    override val cacheZone: CacheZone = CacheZone.FEEDBACK

    override fun loader(): suspend () -> List<CachedUserFeedback> =
        { userFeedbackRepository.findAll().map { talk -> loadCachedUserFeedback(talk) } }

    override fun unitaryLoader(id: String): suspend () -> CachedUserFeedback? =
        { userFeedbackRepository.findOne(id).awaitSingleOrNull()?.let { loadCachedUserFeedback(it) } }

    suspend fun save(talk: UserFeedback): UserFeedback =
        userFeedbackRepository.save(talk)
            .awaitSingle()
            .also {
                // To be more efficient we don't reset the cache here
                val cached = findOneOrNull(talk.id!!)?.updateWith(talk) ?: loadCachedUserFeedback(talk)
                updateElement(cached)
            }

    suspend fun findByEvent(eventId: String): List<CachedUserFeedback> =
        findAll()
            .filter { it.event == eventId }

    suspend fun findByTalk(talkId: String): List<CachedUserFeedback> =
        findAll().filter { it.talk.id == talkId }

    suspend fun findByTalkSlug(slug: String) =
        findAll().first { it.talk.slug == slug }

    suspend fun findByUserEmail(encryptedEmail: String, talkId: String): CachedUserFeedback? =
        findAll()
            .firstOrNull { feedback -> feedback.user.email == encryptedEmail && feedback.talk.id == talkId }

    private suspend fun loadCachedUserFeedback(userFeedback: UserFeedback): CachedUserFeedback {
        val user = userRepository.findByEncryptedEmail(userFeedback.encryptedEmail) ?: throw NotFoundException()
        val talk = talkRepository.findOne(userFeedback.talkId).awaitSingle()
        return CachedUserFeedback(userFeedback, talk, user)
    }

    fun deleteOne(id: String) =
        userFeedbackRepository.deleteOne(id).doOnSuccess { invalidateElement(id) }
}
