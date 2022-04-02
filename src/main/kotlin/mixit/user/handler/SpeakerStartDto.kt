package mixit.user.handler

import mixit.user.model.CachedSpeaker
import mixit.util.camelCase

data class SpeakerStarDto(
    val login: String,
    val key: String,
    val name: String,
    val year: Int,
    val photoUrl: String?
)

fun CachedSpeaker.toSpeakerStarDto() =
    SpeakerStarDto(
        login,
        lastname.lowercase().replace("è", "e").replace(" ", ""),
        "${firstname.camelCase()} ${lastname.camelCase()}",
        year,
        photoUrl
    )
