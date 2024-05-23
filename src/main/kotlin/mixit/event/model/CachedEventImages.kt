package mixit.event.model

import mixit.util.cache.Cached
import mixit.util.mustache.MustacheTemplate

data class CachedEventImages(
    override val id: String,
    val event: String,
    val sections: List<CachedEventImageSection>,
    val rootUrl: String?
) : Cached {
    fun toEventImages(): EventImages =
        EventImages(
            event,
            sections.map {
                EventImagesSection(
                    it.sectionId,
                    it.i18nLabel,
                    it.pictures
                )
            },
            rootUrl
        )

    constructor(images: EventImages) : this(
        images.event.toString(),
        images.event!!,
        images.sections.map { CachedEventImageSection(it) },
        images.rootUrl
    )

    fun toDto(defaultUrl: String): EventImagesDto = EventImagesDto(
        id = id,
        event = event,
        rootUrl = rootUrl ?:defaultUrl,
        sections = sections.map { section ->
            EventImagesSectionDto(
                sectionId = section.sectionId,
                i18nLabel = section.i18nLabel,
                pictures = section.pictures.map {
                    ImageDto(
                        name = it.name,
                        talkId = it.talkId,
                        mustacheTemplate = it.mustacheTemplate,
                        rootUrl = rootUrl ?: defaultUrl
                    )
                }
            )
        }
    )
}

data class CachedEventImageSection(
    val sectionId: String,
    val i18nLabel: String,
    val pictures: List<EventImage>
) {
    constructor(section: EventImagesSection) : this(
        section.sectionId,
        section.i18n,
        section.images
    )
}


data class EventImagesDto(
    val id: String,
    val event: String,
    val sections: List<EventImagesSectionDto>,
    val rootUrl: String
)

data class EventImagesSectionDto(
    val sectionId: String,
    val i18nLabel: String,
    val pictures: List<ImageDto>,
)

data class ImageDto(
    val name: String,
    val talkId: String?,
    val mustacheTemplate: MustacheTemplate?,
    val rootUrl: String
)
