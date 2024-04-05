package mixit.talk.spi.sessionize.dto

import mixit.talk.model.Language
import mixit.talk.model.Language.ENGLISH
import mixit.talk.model.Language.FRENCH
import mixit.talk.model.Room
import mixit.talk.model.TalkFormat
import mixit.talk.model.TalkLevel
import mixit.talk.model.Topic

data class SessionizeCategory(
    val id: Long,
    val title: String,
    val items: List<SessionizeCategoryItem>
) {
    fun isFormat() = title.lowercase().contains("format")
    fun isLevel() = title.lowercase().contains("level")
    fun isLanguage() = title.lowercase().contains("language")
    fun isTrack() = title.lowercase().contains("track")
    fun isRoom() = title.lowercase().contains("room")
}

data class SessionizeCategoryItem(
    val id: Long,
    val name: String,
    val sort: Int
) {
    fun toFormat(): TalkFormat =
        name.lowercase().let {
            when {
                it.contains("workshop") -> TalkFormat.WORKSHOP
                it.contains("lightning") -> TalkFormat.LIGHTNING_TALK
                it.contains("keynote") -> TalkFormat.KEYNOTE
                else -> TalkFormat.TALK
            }
        }

    fun toLanguage(): Language =
        if (name.lowercase().contains("english")) ENGLISH else FRENCH

    fun toLevel(): TalkLevel? =
        name.lowercase().let {
            when {
                it.contains("expert") -> TalkLevel.Expert
                it.contains("advanced") -> TalkLevel.Advanced
                it.contains("beginner") -> TalkLevel.Beginner
                it.contains("intermediate") -> TalkLevel.Intermediate
                else -> null
            }
        }

    fun toTrack(): Topic =
        when (name.lowercase()) {
            "tech" -> Topic.TECH
            "team" -> Topic.TEAM
            "ethics" -> Topic.ETHICS
            "design" -> Topic.DESIGN
            "alien" -> Topic.ALIENS
            "other" -> Topic.OTHER
            else -> Topic.OTHER
        }

    fun toRoom(): Room =
        when (name.lowercase()) {
            "nonaka" -> Room.ROOM4
            "turing" -> Room.ROOM3
            "hamilton" -> Room.AMPHI1
            "lovelace" -> Room.AMPHI2
            "kare" -> Room.ROOM2
            "gosling" -> Room.ROOM1
            "dijkstra" -> Room.ROOM5
            "feinler" -> Room.ROOM6
            "hopper" -> Room.ROOM7
            "mixit on air" -> Room.TWITCH
            else -> Room.UNKNOWN
        }
}

