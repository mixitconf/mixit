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

