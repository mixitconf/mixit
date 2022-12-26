package mixit.event.model

import mixit.routes.MustacheTemplate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class EventImages(
    @Id val event: String?,
    val sections: List<EventImagesSection>
)

data class EventImagesSection(
    val sectionId: String,
    val i18n: String,
    val images: List<EventImage>
)

data class EventImage(
    val name: String,
    val talkId: String? = null,
    val mustacheTemplate: MustacheTemplate? = null
)
