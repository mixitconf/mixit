package mixit.event.model

import mixit.util.cache.Cached

data class CachedEventImages(
    override val id: String,
    val event: Int,
    val sections: List<CachedEventImageSection>
) : Cached {
    fun toEventImages(): EventImages =
        EventImages(
            event,
            sections.map { EventImagesSection(
                it.sectionId,
                it.i18nLabel,
                it.pictures
            ) }
        )

    constructor(images: EventImages) : this(
        images.event.toString(),
        images.event!!,
        images.sections.map { CachedEventImageSection(it) }
    )
}

data class CachedEventImageSection(
    val sectionId: String,
    val i18nLabel: String,
    val pictures: List<EventImage>
) {
    constructor(section: EventImagesSection) : this(section.sectionId, section.i18n, section.images)
}
