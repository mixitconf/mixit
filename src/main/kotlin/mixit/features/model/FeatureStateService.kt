package mixit.features.model

import kotlinx.coroutines.reactor.mono
import mixit.features.repository.FeatureStateRepository
import mixit.util.cache.CacheCaffeineTemplate
import mixit.util.cache.CacheZone
import mixit.util.errors.NotFoundException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class FeatureStateService(
    private val repository: FeatureStateRepository,
) : CacheCaffeineTemplate<CachedFeatureState>() {

    override val cacheZone: CacheZone = CacheZone.FEATURE
    override fun loader(): suspend () -> List<CachedFeatureState> =
        { repository.findAll().map { event -> loadFeatures(event) } }

    suspend fun save(section: FeatureState) =
        repository.save(section).also { invalidateCache() }

    private suspend fun loadFeatures(featureState: FeatureState): CachedFeatureState {
        return CachedFeatureState(
            id = featureState.id!!,
            active = featureState.active,
            feature = featureState.feature
        )
    }

    fun findFeature(feature: Feature): Mono<CachedFeatureState> =
        mono { findAll().firstOrNull { it.feature == feature } }

    suspend fun findOneByType(feature: Feature) =
        findAll().firstOrNull { it.feature == feature } ?: throw NotFoundException()
}
