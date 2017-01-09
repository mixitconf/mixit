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


