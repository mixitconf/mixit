package mixit.event.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class EventImages(
    @Id val event: Int,
    val sections: List<EventImagesSection>
)

data class EventImagesSection(
    val name: String,
    val i18n: String,
    val images: List<String>
)
