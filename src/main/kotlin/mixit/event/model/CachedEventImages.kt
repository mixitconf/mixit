package mixit.event.model

import mixit.util.cache.Cached

data class CachedEventImages(
    override val id: String,
    val event: Int,
    val sections: List<CachedEventImageSection>
) : Cached {
    constructor(images: EventImages) : this(images.event.toString(), images.event, images.sections.map { CachedEventImageSection(it) })
}

data class CachedEventImageSection(
    val name: String,
    val i18nLabel: String,
    val pictures: List<String>
) {
    constructor(section: EventImagesSection) : this(section.name, section.i18n, section.images)
}
