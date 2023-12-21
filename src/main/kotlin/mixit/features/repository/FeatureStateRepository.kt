package mixit.features.repository

import kotlinx.coroutines.reactor.awaitSingle
import mixit.faq.model.QuestionSection
import mixit.features.model.Feature
import mixit.features.model.FeatureState
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.remove
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

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
        }
    }

    fun count() = template.count<FeatureState>()

    suspend fun findAll() = template.findAll<FeatureState>().collectList().awaitSingle()

    fun save(feature: FeatureState) = template.save(feature)

    fun deleteAll() =
        template.remove<FeatureState>(Query())

}
