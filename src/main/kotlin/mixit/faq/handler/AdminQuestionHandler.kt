package mixit.faq.handler

import mixit.MixitProperties
import mixit.faq.model.Question
import mixit.faq.model.QuestionSection
import mixit.faq.model.QuestionSectionService
import mixit.faq.model.Text
import mixit.faq.repository.QuestionSectionRepository
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate.AdminFaq
import mixit.routes.MustacheTemplate.AdminFaqQuestion
import mixit.routes.MustacheTemplate.AdminFaqQuestionSection
import mixit.talk.model.Language
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.seeOther
import mixit.util.validator.MarkdownValidator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
import java.util.*

@Component
class AdminQuestionHandler(
    private val service: QuestionSectionService,
    private val repository: QuestionSectionRepository,
    private val properties: MixitProperties,
    private val markdownValidator: MarkdownValidator
) {

    companion object {
        const val LIST_URI = "/admin/faq"
    }

    suspend fun viewFaqAdmin(req: ServerRequest): ServerResponse {
        val elements = service.findAll().sortedBy { it.order }
        return ok().renderAndAwait(
            AdminFaq.template,
            mapOf(TITLE to AdminFaq.title, "sections" to elements)
        )
    }

    suspend fun createFaqSectionAdmin(req: ServerRequest): ServerResponse {
        return openFaqSectionAdmin(null)
    }

    suspend fun viewFaqSectionAdmin(req: ServerRequest): ServerResponse {
        val sectionId = req.pathVariable("sectionId")
        val section = service
            .findAll()
            .firstOrNull { it.id == sectionId }
            ?.let { section -> section.toQuestionSection().copy(questions = section.questions.sortedBy { it.order }) }
            ?: throw NotFoundException()
        return openFaqSectionAdmin(section)
    }

    private suspend fun openFaqSectionAdmin(section: QuestionSection?): ServerResponse {
        return ok().renderAndAwait(
            AdminFaqQuestionSection.template,
            mapOf(TITLE to AdminFaq.title, "section" to section)
        )
    }

    suspend fun createFaqQuestionAdmin(req: ServerRequest): ServerResponse {
        return openFaqQuestionAdmin(req.pathVariable("sectionId"), null)
    }

    suspend fun viewFaqQuestionAdmin(req: ServerRequest): ServerResponse {
        val questionId = req.pathVariable("questionId")
        val sectionId = req.pathVariable("sectionId")
        val question = service
            .findAll()
            .flatMap { it.questions }
            .firstOrNull { it.id == questionId }
            ?: throw NotFoundException()
        return openFaqQuestionAdmin(sectionId, question)
    }

    private suspend fun openFaqQuestionAdmin(sectionId: String, question: Question?): ServerResponse {
        return ok().renderAndAwait(
            AdminFaqQuestion.template,
            mapOf(TITLE to AdminFaq.title, "question" to question, "sectionId" to sectionId)
        )
    }

    suspend fun saveFaqSectionAdmin(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val existing = repository.findOneOrNull(formData["sectionId"])

        val section = existing
            ?.copy(
                title = Text(
                    descriptionFr = formData["titleFr"]!!,
                    descriptionEn = formData["titleEn"]!!
                ),
                order = formData["order"]?.toInt() ?: 0
            )
            ?: QuestionSection(
                title = Text(
                    descriptionFr = formData["titleFr"]!!,
                    descriptionEn = formData["titleEn"]!!
                ),
                questions = emptyList(),
                order = formData["order"]?.toInt() ?: 0,
                id = UUID.randomUUID().toString()
            )
        service.save(section)
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    suspend fun saveFaqQuestionAdmin(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val section = repository.findOneOrNull(formData["sectionId"]) ?: throw NotFoundException()
        val existing = section.questions.firstOrNull { it.id == formData["questionId"] }

        val newSection = if (existing == null) {
            val newQuestion = Question(
                id = UUID.randomUUID().toString(),
                title = Text(
                    descriptionFr = formData["titleFr"]!!,
                    descriptionEn = formData["titleEn"]!!
                ),
                answer = Text(
                    descriptionFr = markdownValidator.sanitize(formData["answerFr"]!!.trim()),
                    descriptionEn = markdownValidator.sanitize(formData["answerEn"]!!.trim())
                ),
                order = formData["order"]?.toInt() ?: 0,
            )
            service.save(
                section.copy(questions = section.questions + newQuestion)
            )
        } else {
            val updatedQuestion = existing.copy(
                title = Text(
                    descriptionFr = formData["titleFr"]!!,
                    descriptionEn = formData["titleEn"]!!
                ),
                answer = Text(
                    descriptionFr = formData["answerFr"]!!,
                    descriptionEn = formData["answerEn"]!!
                ),
                order = formData["order"]?.toInt() ?: 0
            )
            service.save(
                section.copy(questions = section.questions.map { if (it.id == updatedQuestion.id) updatedQuestion else it })
            )
        }
        return openFaqSectionAdmin(newSection)
    }

    suspend fun deleteFaqSectionAdmin(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        service.deleteOne(formData["sectionId"] ?: throw NotFoundException())
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    suspend fun deleteFaqQuestionAdmin(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val sectionId = formData["sectionId"] ?: throw NotFoundException()
        val section = repository.findOneOrNull(sectionId) ?: throw NotFoundException()
        service.save(section.copy(questions = section.questions.filter { it.id != formData["questionId"] }))
        return seeOther("${properties.baseUri}$LIST_URI/sections/${sectionId}")
    }
}
