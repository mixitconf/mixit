package mixit.blog.model

import mixit.talk.model.Language
import mixit.util.toSlug
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document
data class Post(
    val authorId: String,
    val addedAt: LocalDateTime = LocalDateTime.now(),
    @TextIndexed(weight = 10F) val title: Map<Language, String> = emptyMap(),
    val headline: Map<Language, String> = emptyMap(),
    val content: Map<Language, String>? = emptyMap(),
    val year: Int? = null,
    @Id val id: String? = null,
    val slug: Map<Language, String> = title.entries.map { (k, v) -> Pair(k, v.toSlug()) }.toMap(),
    // We can't add an index on Map values. This field is only used to be able to do that
    @TextIndexed(weight = 5F) val indexedHeadline: String = headline.values.joinToString(" "),
    @TextIndexed(weight = 5F) val indexedContent: String = if (content == null) " " else content.values.joinToString(" ")

)
