package mixit.talk.model

import java.time.LocalDateTime
import mixit.user.model.Link
import mixit.util.toSlug
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Talk(
    val format: TalkFormat,
    val event: String,
    @TextIndexed(weight = 10F) val title: String,
    @TextIndexed(weight = 5F) val summary: String,
    val speakerIds: List<String> = emptyList(),
    val language: Language = Language.FRENCH,
    val addedAt: LocalDateTime = LocalDateTime.now(),
    @TextIndexed val description: String? = null,
    val topic: String? = null,
    val video: String? = null,
    val video2: String? = null,
    val room: Room? = Room.UNKNOWN,
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null,
    val photoUrls: List<Link> = emptyList(),
    val slug: String = title.toSlug(),
    val level: TalkLevel? = null,
    @Id val id: String? = null
)




