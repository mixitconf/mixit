package mixit.talk.spi.sessionize.dto

import mixit.talk.model.Room

data class SessionizeRoom(
    val id: String,
    val name: String,
    val sort: Int
) {
    fun toRoom(): Room =
        if (name.lowercase().contains("mixit on air")) Room.TWITCH
        else when (name.lowercase()) {
            "nonaka" -> Room.ROOM4
            "turing" -> Room.ROOM3
            "hamilton" -> Room.AMPHI1
            "lovelace" -> Room.AMPHI2
            "kare" -> Room.ROOM2
            "gosling" -> Room.ROOM1
            "djikstra" -> Room.ROOM5
            "feinler" -> Room.ROOM6
            "hopper" -> Room.ROOM7
            "mixit on air" -> Room.TWITCH
            else -> Room.UNKNOWN
        }
}

