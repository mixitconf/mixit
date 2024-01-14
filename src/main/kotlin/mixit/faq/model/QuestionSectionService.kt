package mixit.faq.model

import mixit.faq.repository.QuestionSectionRepository
import mixit.util.cache.CacheCaffeineTemplate
import mixit.util.cache.CacheZone
import org.springframework.stereotype.Service

@Service
class QuestionSectionService(
    private val repository: QuestionSectionRepository
) : CacheCaffeineTemplate<CachedQuestionSection>() {

    override val cacheZone: CacheZone = CacheZone.FEATURE
    override fun loader(): suspend () -> List<CachedQuestionSection> =
        { repository.findAll().map { event -> loadQuestionSections(event) } }

    suspend fun save(section: QuestionSection) =
        repository.save(section).also { cache.invalidateAll() }

    private suspend fun loadQuestionSections(section: QuestionSection): CachedQuestionSection {
        return CachedQuestionSection(
            id = section.id!!,
            title = section.title,
            questions = section.questions,
            order = section.order
        )
    }

    suspend fun deleteOne(id: String) =
        repository.deleteOne(id).also {
            cache.invalidateAll()
        }
}
