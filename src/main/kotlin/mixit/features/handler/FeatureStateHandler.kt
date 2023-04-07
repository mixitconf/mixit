package mixit.features.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.features.model.Feature
import mixit.features.repository.FeatureStateRepository
import mixit.routes.MustacheI18n.FEATURES
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate
import mixit.util.extractFormData
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class FeatureStateHandler(
    private val featureStateRepository: FeatureStateRepository
) {

    suspend fun list(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(
            MustacheTemplate.AdminFeatureState.template,
            mapOf(TITLE to MustacheTemplate.AdminFeatureState.title, FEATURES to featureStateRepository.findAll())
        )

    suspend fun save(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        return Feature.values()
            .onEach {
                val active = formData[it.name] == "on"
                val state = featureStateRepository.findOneByType(it)
                featureStateRepository.save(state.copy(active = active)).block()
            }
            .let {
                list(req)
            }
    }

}
