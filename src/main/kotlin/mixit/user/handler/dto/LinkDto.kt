package mixit.user.handler.dto

import mixit.user.model.Link
import mixit.user.model.User
import java.util.stream.IntStream

data class LinkDto(
    val name: String,
    val url: String,
    val index: String
) {
    val image =
        if (name.contains("twitter", true)) "mxt-icon--social2.svg"
        else if (name.contains("gitHub", true)) "mxt-icon--social3.svg"
        else if (name.contains("linkedin", true)) "mxt-icon--social4.svg"
        else if (name.contains("sky", true)) "mxt-icon--social-bsky.svg"
        else if (name.contains("facebook", true)) "mxt-icon--social1.svg"
        else if (name.contains("instagram", true)) "mxt-icon--social-instagram.svg"
        else if (name.contains("youtube", true)) "mxt-icon--social-youtube.svg"
        else if (name.contains("medium", true)) "mxt-icon--social-medium.svg"
        else if (name.contains("twitch", true)) "mxt-icon--social-twitch.svg"
        else if (name.contains("tiktok", true)) "mxt-icon--social-tiktok.svg"
        else if (name.contains("discord", true)) "mxt-icon--social-discord.svg"
        else if (name.contains("snapchat", true)) "mxt-icon--social-snapchat.svg"
        else if (name.contains("whatsapp", true)) "mxt-icon--social-whatsapp.svg"
        else if (name.contains("pinterest", true)) "mxt-icon--social-pinterest.svg"
        else if (name.contains("tumblr", true)) "mxt-icon--social-tumblr.svg"
        else if (name.contains("reddit", true)) "mxt-icon--social-reddit.svg"
        else "mxt-icon--social-link-solid.svg"
}

fun Link.toLinkDto(index: Int) =
    LinkDto(name, url, "link${index + 1}")

fun User.toLinkDtos(): Map<String, List<LinkDto>> =
    if (links.size > 4) {
        links.mapIndexed { index, link -> link.toLinkDto(index) }.groupBy { it.index }
    } else {
        val existingLinks = links.size
        val userLinks = links.mapIndexed { index, link -> link.toLinkDto(index) }.toMutableList()
        IntStream.range(0, 5 - existingLinks)
            .forEach { userLinks.add(LinkDto("", "", "link${existingLinks + it + 1}")) }
        userLinks.groupBy { it.index }
    }
