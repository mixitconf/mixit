package mixit.event.model

import mixit.user.model.Link

data class EventDto(
    val id: String,
    val start: String,
    val end: String,
    val current: Boolean,
    val photoUrls: List<Link> = emptyList(),
    val videoUrl: Link? = null,
    val schedulingFileUrl: String? = null,
    val year: Int
)
