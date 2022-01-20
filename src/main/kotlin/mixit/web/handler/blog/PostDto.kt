package mixit.web.handler.blog

import mixit.model.Language
import mixit.model.Post
import mixit.model.User
import mixit.util.formatDate
import mixit.util.markFoundOccurrences
import mixit.util.toRFC3339

data class PostDto(
    val id: String?,
    val slug: String,
    val author: User,
    val addedAt: String,
    val title: String,
    val headline: String,
    val content: String?
)

fun Post.toDto(author: User, language: Language, searchTerms: List<String> = emptyList()) = PostDto(
    id,
    slug[language] ?: "",
    author,
    addedAt.formatDate(language),
    title[language] ?: "".markFoundOccurrences(searchTerms),
    headline[language] ?: "".markFoundOccurrences(searchTerms),
    if (content != null) content[language]?.markFoundOccurrences(searchTerms) else null
)

class Feed(
    val title: String,
    val link: String,
    val updated: String,
    val entries: List<FeedEntry>
)

class FeedEntry(
    val id: String,
    val title: String,
    val link: String,
    val updated: String
)

fun Post.toFeedEntry(language: Language) = FeedEntry(
    id!!,
    title[language]!!,
    slug[language]!!,
    addedAt.toRFC3339()
)

fun List<Post>.toFeed(language: Language, title: String, link: String) = Feed(
    title,
    link,
    first().addedAt.toRFC3339(),
    map { it.toFeedEntry(language) }
)