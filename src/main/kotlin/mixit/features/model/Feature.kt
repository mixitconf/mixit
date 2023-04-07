package mixit.features.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

enum class Feature {
    MixitOnAirOnHomePage
}

@Document
data class FeatureState(
    val active: Boolean,
    val feature: Feature,
    @Id val id: String? = null,
)
