package mixit.talk.spi.sessionize.dto

import mixit.talk.model.Language
import mixit.talk.model.Room
import mixit.talk.model.TalkFormat
import mixit.talk.model.TalkLevel
import mixit.talk.model.Topic

data class SessionizeResponse(
    val sessions: List<SessionizeSession>,
    val speakers: List<SessionizeSpeaker>,
    val categories: List<SessionizeCategory>,
    val rooms: List<SessionizeRoom>,
) {
    fun formats(): Map<Long, TalkFormat> {
        val formats = categories.filter { it.isFormat() }.flatMap { it.items }
        return formats.associate { it.id to it.toFormat() }
    }

    fun languages(): Map<Long, Language> {
        val languages = categories.filter { it.isLanguage() }.flatMap { it.items }
        return languages.associate {
            it.id to it.toLanguage()
        }
    }

    fun topics(): Map<Long, Topic> {
        val tracks = categories.filter { it.isTrack() }.flatMap { it.items }
        return tracks.associate {
            it.id to it.toTrack()
        }
    }

    fun levels(): Map<Long, TalkLevel?> {
        val levels = categories.filter { it.isLevel() }.flatMap { it.items }
        return levels.associate {
            it.id to it.toLevel()
        }
    }

    fun rooms(): Map<String, Room> =
        rooms.associate {
            it.id to it.toRoom()
        }
}
