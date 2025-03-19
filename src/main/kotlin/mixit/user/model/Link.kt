package mixit.user.model

data class Link(
    val name: String,
    val url: String
)

fun Link.isTwitter(): Boolean =
    url.contains("twitter") || url.contains("x.org") || url.contains("x.com")
