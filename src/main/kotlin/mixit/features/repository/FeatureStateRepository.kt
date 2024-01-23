package mixit.features.repository

import kotlinx.coroutines.reactor.awaitSingle
import mixit.features.model.Feature
import mixit.features.model.FeatureState
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository

@Repository
class FeatureStateRepository(private val template: ReactiveMongoTemplate) {

    fun initData() {
        if (count().block() == 0L) {
            save(FeatureState(false, Feature.Cfp)).block()
            save(FeatureState(false, Feature.Lottery)).block()
            save(FeatureState(false, Feature.LotteryResult)).block()
            save(FeatureState(false, Feature.Mixette)).block()
            save(FeatureState(false, Feature.MixitOnAirOnHomePage)).block()
            save(FeatureState(false, Feature.ProfileMessage)).block()
            save(FeatureState(false, Feature.Feedback)).block()
        }
    }

    fun count() = template.count<FeatureState>()

    suspend fun findAll(): List<FeatureState> {
        val dbFeatures = template.findAll<FeatureState>().collectList().awaitSingle()
        val notDbFeatures = (Feature.entries - dbFeatures.map { it.feature }.toSet())
            .map { FeatureState(false, it, it.name) }
        return dbFeatures + notDbFeatures
    }

    fun save(feature: FeatureState) = template.save(feature)

    fun deleteAll() =
        template.remove<FeatureState>(Query())
}
