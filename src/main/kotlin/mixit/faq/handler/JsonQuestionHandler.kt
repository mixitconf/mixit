package mixit.faq.handler

import mixit.faq.repository.QuestionSectionRepository
import mixit.util.json
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class JsonQuestionHandler(
    private val repository: QuestionSectionRepository
) {
    suspend fun findAll(req: ServerRequest): ServerResponse =
        repository.findAll().let { ok().json().bodyValueAndAwait(it) }

}
