package mixit.faq.handler

import mixit.event.model.EventService
import mixit.faq.model.QuestionSectionService
import mixit.user.model.UserService
import mixit.util.SimpleTemplateLoader
import mixit.util.mustache.MustacheTemplate.Faq
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class FaqHandler(
    private val questionSectionService: QuestionSectionService,
    private val eventService: EventService,
    private val userService: UserService
) {

    suspend fun faqView(req: ServerRequest): ServerResponse =
        SimpleTemplateLoader.openTemplate(
            eventService,
            userService,
            Faq,
            mapOf(
                "sections" to questionSectionService
                    .findAll()
                    .map { section -> section.copy(questions = section.questions.sortedBy { it.order }) }
                    .sortedBy { it.order }
            )
        )
}
