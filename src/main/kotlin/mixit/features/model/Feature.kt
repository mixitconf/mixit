package mixit.features.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

enum class Feature {
    /**
     * Active MiXiT on Air on the home page
     */
    MixitOnAirOnHomePage,

    /**
     * Active the lottery feature
     */
    Lottery,

    /**
     * If active we display the lottery result in user profile
     */
    LotteryResult,

    /**
     * We have a double check on the user profile page to display lottery result
     */
    ProfileMessage,

    /**
     * Active the mixette feature
     */
    Mixette,
    /**
     * Active the CFP
     */
    Cfp,
    /**
     * Active talk feedback
     */
    Feedback,
}

@Document
data class FeatureState(
    val active: Boolean,
    val feature: Feature,
    @Id val id: String? = null,
)
