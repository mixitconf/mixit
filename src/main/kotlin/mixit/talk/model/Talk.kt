package mixit.talk.model

import mixit.user.model.Link
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
enum class Room(capacity: Int, val hasLink: Boolean) {
    AMPHI1(500, true),
    AMPHI2(200, true),
    AMPHIC(445,false),
    AMPHID(445,false),
    AMPHIK(300,false),
    ROOM1(48,true),
    ROOM2(48,true),
    ROOM3(36,true),
    ROOM4(36,true),
    ROOM5(36,true),
    ROOM6(36,true),
    ROOM7(36,true),
    ROOM8(36,false),
    OUTSIDE(50,false),
    MUMMY(30,false),
    SPEAKER(16,false),
    UNKNOWN(0,false),
    SURPRISE(0,false);
}

enum class Topic(val value: String) {
    MAKERS("makers"),
    ALIENS("aliens"),
    TECH("tech"),
    TEAM("team"),
    OTHER("other"),
    DESIGN("design"),
    HACK("hacktivism"),
    ETHICS("ethics"),
    LIFE_STYLE("lifestyle"),
    LEARN("learn");

    companion object {
        fun of(label: String) = values().firstOrNull { it.value == label } ?: OTHER
    }
}
