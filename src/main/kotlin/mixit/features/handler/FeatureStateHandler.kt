package mixit.features.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.features.model.Feature
import mixit.features.model.FeatureStateService
import mixit.routes.MustacheI18n.FEATURES
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class FeatureStateHandler(
    private val featureStateService: FeatureStateService
) {

    suspend fun list(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(
            MustacheTemplate.AdminFeatureState.template,
            mapOf(TITLE to MustacheTemplate.AdminFeatureState.title, FEATURES to featureStateService.findAll())
        )

    suspend fun save(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        return Feature.entries
            .map {
                val active = formData[it.name] == "on"
                val state = featureStateService.findOneByType(it).toFeature()
                featureStateService.save(state.copy(active = active)).awaitSingle()
            }
            .let {
                list(req)
            }
    }
}
