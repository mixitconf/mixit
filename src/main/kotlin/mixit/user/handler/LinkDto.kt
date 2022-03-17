package mixit.user.handler

import mixit.user.model.Link
import mixit.user.model.User
import java.util.stream.IntStream

data class LinkDto(val name: String, val url: String, val index: String)

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
