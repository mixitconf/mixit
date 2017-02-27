package mixit.data.dto

import mixit.model.Language
import mixit.model.Room
import mixit.model.Session
import mixit.model.SessionFormat
import java.time.LocalDateTime

data class SessionDataDto(
    val idSession: Long,
    var year: Int,
    var format: String,
    var title: String,
    var speakers: Iterable<MemberDataDto> = emptyList(),
    var lang: String,
    var summary: String,
    var description: String? = null,
    var video: String? = null,
    var room: String ? = null,
    var start: LocalDateTime ? = null,
    var end: LocalDateTime ? = null

) {
    fun toSession(): Session {
        return Session(
                SessionFormat.valueOf(format.toUpperCase()),
                "mixit${year.toString().substring(2, 4)}",
                title,
                summary,
                speakers.map { speaker -> speaker.toUser() },
                if (lang == "en") Language.ENGLISH else Language.FRENCH,
                LocalDateTime.now(),
                description,
                video,
                Room.findByName(room ?: ""),
                start,
                end,
                "$idSession"
        )
    }
}
