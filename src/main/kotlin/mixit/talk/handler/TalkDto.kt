package mixit.talk.handler

import java.time.LocalDateTime
import mixit.MixitApplication
import mixit.talk.model.TalkFormat
import mixit.talk.model.TalkLevel
import mixit.user.handler.dto.UserDto
import mixit.user.model.Link

class TalkDto(
    val id: String?,
    val slug: String,
    val format: TalkFormat,
    val event: String,
    val title: String,
    val summary: String,
    val speakers: List<UserDto>,
    val language: String,
    val addedAt: LocalDateTime,
    val description: String?,
    val topic: String?,
    val video: String?,
    val video2: String?,
    val isYoutube: Boolean?,
    val isVimeo: Boolean?,
    val player: String?,
    val room: String?,
    val roomLink: String?,
    val start: String?,
    val end: String?,
    val date: String?,
    val favorite: Boolean = false,
    val photoUrls: List<Link> = emptyList(),
    val isEn: Boolean = (language == "english"),
    val isTalk: Boolean = (format == TalkFormat.TALK),
    val isCurrentEdition: Boolean = MixitApplication.CURRENT_EVENT == event,
    val multiSpeaker: Boolean = (speakers.size > 1),
    val speakersFirstNames: String = (speakers.joinToString { it.firstname }),
    val startLocalDateTime: LocalDateTime? = null,
    val endLocalDateTime: LocalDateTime? = null,
    val hasAtLeastOneVideo: Boolean = !video.isNullOrEmpty() || !video2.isNullOrEmpty(),
    val level: TalkLevel? = null,
    val onair: Boolean = (format == TalkFormat.ON_AIR),
    val hasFeedback: Boolean = event.toInt() >= 2024,
)

data class DayTalksDto(
    val id: String,
    val day: String,
    val active: Boolean,
    val talks: List<TalkDto>
)

data class DayRoomTalksDto(
    val id: String,
    val day: String,
    val active: Boolean,
    val slices: List<RoomDaySliceDto>
)

data class RoomDaySliceDto(val room: String?, val talkByRooms: List<RoomTalkDto>)

data class RoomTalkDto(
    val time: String,
    val start: LocalDateTime,
    val timeDisplayed: Boolean,
    val bordered: Boolean,
    val sliceDuration: Long = 1,
    val talk: TalkDto?
)
