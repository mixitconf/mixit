package mixit.talk.spi.sessionize.dto

import mixit.talk.model.Language
import mixit.talk.model.TalkFormat
import mixit.talk.model.Topic

data class SessionizeResponse(
    val sessions: List<SessionizeSession>,
    val speakers: List<SessionizeSpeaker>,
    val categories: List<SessionizeCategory>
) {
    fun formats(): Map<Long, TalkFormat> {
        val formats = categories.filter { it.isFormat() }.flatMap { it.items }
        return formats.associate {
            val format = if (it.name.lowercase().contains("workshop")) {
                TalkFormat.WORKSHOP
            } else if (it.name.lowercase().contains("lightning")) {
                TalkFormat.LIGHTNING_TALK
            } else if (it.name.lowercase().contains("keynote")) {
                TalkFormat.KEYNOTE
            } else {
                TalkFormat.TALK
            }
            it.id to format
        }
    }

    fun languages(): Map<Long, Language> {
        val languages = categories.filter { it.isLanguage() }.flatMap { it.items }
        return languages.associate {
            val language = if (it.name.lowercase().contains("english")) {
                Language.ENGLISH
            } else {
                Language.FRENCH
            }
            it.id to language
        }
    }

    fun topics(): Map<Long, Topic> {
        val tracks = categories.filter { it.isTrack() }.flatMap { it.items }
        return tracks.associate {
            val topic = when (it.name.lowercase()) {
                "tech" -> Topic.TECH
                "team" -> Topic.TEAM
                "ethics" -> Topic.ETHICS
                "design" -> Topic.DESIGN
                "alien" -> Topic.ALIENS
                "other" -> Topic.OTHER
                else -> Topic.OTHER
            }
            it.id to topic
        }
    }
}
