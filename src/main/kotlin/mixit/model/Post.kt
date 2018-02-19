package mixit.model

import mixit.util.toSlug
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime


@Document
data class Post(
        val authorId: String,
        val addedAt: LocalDateTime = LocalDateTime.now(),
        @TextIndexed val title: Map<Language, String> = emptyMap(),
        @TextIndexed(weight = 5F) val headline: Map<Language, String> = emptyMap(),
        @TextIndexed(weight = 10F) val content: Map<Language, String>? = emptyMap(),
        @Id val id: String? = null,
        val slug: Map<Language, String> = title.entries.map { (k, v) -> Pair(k, v.toSlug()) }.toMap()
)