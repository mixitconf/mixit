package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document
data class Article (
        val author: User,
        val addedAt: LocalDateTime = LocalDateTime.now(),
        val title: Map<Language, String> = emptyMap(),
        val content: Map<Language, String> = emptyMap(),
        @Id val id: String? = null
)