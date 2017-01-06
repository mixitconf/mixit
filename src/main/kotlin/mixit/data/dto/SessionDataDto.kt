package mixit.data.dto

import mixit.model.*
import java.time.LocalDateTime

data class SessionDataDto(
    val idSession: Long,
    var year: Int,
    var format: SessionFormat,
    var title: String,
    var speakers: Iterable<MemberDataDto> = emptyList(),
    var lang: SessionLanguage,
    var summary: String? = null,
    var description: String? = null,
    var room: String ? = null,
    var start: LocalDateTime ? = null,
    var end: LocalDateTime ? = null

){
    fun toSession(): Session {
        return Session(
                format,
                year,
                title,
                summary ?: "",
                speakers.map { speaker -> speaker.toSessionSpeaker() },
                lang?: SessionLanguage.fr,
                LocalDateTime.now(),
                description,
                Room.findByName(room ?: ""),
                start,
                end,
                "${idSession}"
        )
    }
}
