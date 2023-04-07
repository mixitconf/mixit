package mixit.features.repository

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import mixit.features.model.Feature
import mixit.features.model.FeatureState
import mixit.mixette.model.MixetteDonation
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
class FeatureStateRepository(private val template: ReactiveMongoTemplate) {

    fun initData() {
        if (count().block() == 0L) {
            save(FeatureState(false, Feature.MixitOnAirOnHomePage)).block()
        }
    }

    fun count() = template.count<FeatureState>()


    suspend fun findOne(id: String): FeatureState =
        template.findById<FeatureState>(id).awaitSingle()

    fun findFeature(feature: Feature): Mono<FeatureState> =
        template
            .findOne<FeatureState>(Query(Criteria.where("feature").isEqualTo(feature.name)))

    suspend fun findOneByType(feature: Feature): FeatureState =
        findFeature(feature).awaitSingle()

    fun findAll() = template.findAll<FeatureState>()

    fun deleteOne(id: String) = template.remove<FeatureState>(Query(where("_id").isEqualTo(id)))

    fun save(feature: FeatureState) = template.save(feature)
}
