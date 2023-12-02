package mixit.faq.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitApplication
import mixit.event.model.EventService
import mixit.faq.model.QuestionSectionService
import mixit.routes.MustacheI18n.SPONSORS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheI18n.YEAR
import mixit.routes.MustacheI18n.YEAR_SELECTOR
import mixit.routes.MustacheTemplate.About
import mixit.routes.MustacheTemplate.Accessibility
import mixit.routes.MustacheTemplate.CodeOfConduct
import mixit.routes.MustacheTemplate.Faq
import mixit.routes.MustacheTemplate.Search
import mixit.routes.MustacheTemplate.Venue
import mixit.user.model.UserService
import mixit.util.YearSelector
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class FaqHandler(
    private val questionSectionService: QuestionSectionService
) {

    suspend fun faqView(req: ServerRequest): ServerResponse =
        ok()
            .render(
                Faq.template,
                mapOf(TITLE to Faq.title, "sections" to questionSectionService.findAll())
            )
            .awaitSingle()


}
