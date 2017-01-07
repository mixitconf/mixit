package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document
data class Session(
        val format: SessionFormat,
        val year: Int,
        val title: String,
        val summary: String,
        val speakers: Iterable<SessionSpeaker> = emptyList(),
        val lang: SessionLanguage = SessionLanguage.fr,
        val addedAt: LocalDateTime,
        val description: String? = null,
        val room: Room? = null,
        val start: LocalDateTime? = null,
        val end: LocalDateTime? = null,
        @Id
        val id: String? = null
)

enum class SessionFormat(val duration: Int) {
    Talk(50),
    LightningTalk(5),
    Workshop(110),
    Random(25),
    Keynote(25)
}

enum class SessionLanguage {
    fr,
    en
}

@Document
data class SessionSpeaker(
        @Id var id: String,
        var firstname: String,
        var lastname: String
)

