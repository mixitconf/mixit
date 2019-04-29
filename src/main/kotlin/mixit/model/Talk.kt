package mixit.model

import mixit.util.toSlug
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime


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
        val room: Room? = Room.UNKNOWN,
        val start: LocalDateTime? = null,
        val end: LocalDateTime? = null,
        val photoUrls: List<Link> = emptyList(),
        val slug: String = title.toSlug(),
        @Id val id: String? = null
)

enum class TalkFormat(val duration: Int) {
    TALK(50),
    LIGHTNING_TALK(5),
    WORKSHOP(110),
    RANDOM(25),
    KEYNOTE(25),
    KEYNOTE_SURPRISE(25),
    CLOSING_SESSION(25)
}

@Suppress("UNUSED_PARAMETER")
enum class Room(capacity: Int) {
    AMPHI1(500),
    AMPHI2(200),
    AMPHIC(445),
    AMPHID(445),
    AMPHIK(300),
    ROOM1(198),
    ROOM2(108),
    ROOM3(30),
    ROOM4(30),
    ROOM5(30),
    ROOM6(30),
    ROOM7(30),
    MUMMY(30),
    SPEAKER(16),
    UNKNOWN(0);
}