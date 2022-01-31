package mixit.user.handler

import mixit.user.cache.CachedSpeaker
import mixit.util.camelCase

data class SpeakerStarDto(
    val login: String,
    val key: String,
    val name: String
)

fun CachedSpeaker.toSpeakerStarDto() =
    SpeakerStarDto(
        login,
        lastname.lowercase().replace("è", "e"),
        "${firstname.camelCase()} ${lastname.camelCase()}"
    )
