package mixit.blog.model

import mixit.blog.handler.Feed
import mixit.blog.handler.FeedEntry
import mixit.talk.model.Language
import mixit.user.model.CachedStaff
import mixit.user.model.User
import mixit.util.cache.Cached
import mixit.util.toRFC3339
import java.time.LocalDateTime


data class CachedPost(
    override val id: String,
    val author: CachedStaff,
    val addedAt: LocalDateTime,
    val title: Map<Language, String>,
    val headline: Map<Language, String>,
    val content: Map<Language, String>,
    val slug: Map<Language, String>
) : Cached {
    constructor(post: Post, user: User) : this(
        post.id!!,
        CachedStaff(user),
        post.addedAt,
        post.title,
        post.headline,
        post.content ?: emptyMap(),
        post.slug
    )

    fun toFeedEntry(language: Language) = FeedEntry(
        id,
        title[language] ?: "",
        slug[language] ?: "",
        addedAt.toRFC3339()
    )

    fun toPost(): Post = Post(
        author.login,
        addedAt,
        title,
        headline,
        content,
        id,
        slug
    )
}

fun List<CachedPost>.toFeed(language: Language, title: String, link: String) = Feed(
    title,
    link,
    first().addedAt.toRFC3339(),
    map { it.toFeedEntry(language) }
)

