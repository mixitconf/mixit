package mixit.model

import mixit.util.MarkdownConverter
import mixit.util.format
import mixit.util.toSlug
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime


@Document
data class Post(
        val author: User,
        val addedAt: LocalDateTime = LocalDateTime.now(),
        val title: Map<Language, String> = emptyMap(),
        val headline: Map<Language, String> = emptyMap(),
        val content: Map<Language, String>? = emptyMap(),
        @Id val id: String? = null,
        val slug: Map<Language, String> = title.entries.map { (k, v) -> Pair(k, v.toSlug()) }.toMap()
)

class PostDto(
    val id: String?,
    val slug: String,
    val author: User,
    val addedAt: String,
    val title: String,
    val headline: String,
    val content: String
)

fun Post.toDto(language: Language, markdownConverter: MarkdownConverter) = PostDto(
        id,
        slug[language] ?: "",
        author,
        addedAt.format(language),
        title[language] ?: "",
        markdownConverter.toHTML(headline[language] ?: ""),
        markdownConverter.toHTML(if (content != null) content[language] else  ""))
