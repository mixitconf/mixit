package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document
data class Session(
        val format: SessionFormat,
        val event: String,
        val title: String,
        val summary: String,
        // TODO Use @DBRref on speakers when https://jira.spring.io/browse/DATAMONGO-1584 will be fixed
        val speakers: List<User> = emptyList(),
        val language: Language = Language.FRENCH,
        val addedAt: LocalDateTime = LocalDateTime.now(),
        val description: String? = null,
        val room: Room? = null,
        val start: LocalDateTime? = null,
        val end: LocalDateTime? = null,
        @Id val id: String? = null
)

enum class SessionFormat(val duration: Int) {
    TALK(50),
    LIGHTNING_TALK(5),
    WORKSHOP(110),
    RANDOM(25),
    KEYNOTE(25)
}

enum class Room(name: String, capacity: Int) {
    AMPHI1("Grand Amphi", 500),
    AMPHI2("Petit Amphi", 200),
    ROOM1("Gosling", 110),
    ROOM2("Eich", 110),
    ROOM3("Nonaka", 30),
    ROOM4("Dijkstra", 30),
    ROOM5("Turing", 30),
    ROOM6("Lovelace", 30),
    ROOM7("Mezzanine", 50),
    UNKNOWN("Inconnue", 0);

    companion object {
        fun findByName(name: String): Room {
            val room = Room.values().filter { value -> value.name == name }
            if (room.isEmpty()) {
                return UNKNOWN
            }
            return room.first()
        }
    }
}


