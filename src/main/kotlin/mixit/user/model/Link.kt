package mixit.user.model

data class Link(
    val name: String,
    val url: String
) {
    companion object {
        val excludedSocialNetworks = listOf("twitter", "x.org", "x.com", "truthsocial", "truth social")
    }
}


fun Link.isTwitterOrTruthSocial(): Boolean =
    Link.excludedSocialNetworks.any { url.lowercase().contains(it) || name.lowercase().contains(it) }

