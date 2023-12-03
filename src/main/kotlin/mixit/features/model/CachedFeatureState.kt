package mixit.features.model

import mixit.util.cache.Cached

data class CachedFeatureState(
    override val id: String,
    val active: Boolean,
    val feature: Feature
) : Cached {
    fun toFeature(): FeatureState =
        FeatureState(
            active = active,
            feature = feature,
            id = id
        )
}
