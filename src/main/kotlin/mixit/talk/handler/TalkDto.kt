package mixit.talk.handler

import java.time.LocalDateTime
import mixit.talk.model.Language
import mixit.talk.model.Talk
import mixit.talk.model.TalkFormat
import mixit.user.model.Link
import mixit.user.model.User
import mixit.util.formatTalkDate
import mixit.util.formatTalkTime
import mixit.util.markFoundOccurrences

class TalkDto(
    val id: String?,
    val slug: String,
    val format: TalkFormat,
    val event: String,
    val title: String,
    val summary: String,
    val speakers: List<User>,
    val language: String,
    val addedAt: LocalDateTime,
    val description: String?,
    val topic: String?,
    val video: String?,
    val vimeoPlayer: String?,
    val room: String?,
    val start: String?,
    val end: String?,
    val date: String?,
    val favorite: Boolean = false,
    val photoUrls: List<Link> = emptyList(),
    val isEn: Boolean = (language == "english"),
    val isTalk: Boolean = (format == TalkFormat.TALK),
    val isCurrentEdition: Boolean = "2019".equals(event),
    val multiSpeaker: Boolean = (speakers.size > 1),
    val speakersFirstNames: String = (speakers.joinToString { it.firstname })
)

fun Talk.toDto(lang: Language, speakers: List<User>, favorite: Boolean = false, convertRandomLabel: Boolean = SURPRISE_RANDOM, searchTerms: List<String> = emptyList()) = TalkDto(
    id, slug, format, event,
    title(convertRandomLabel, searchTerms),
    summary(convertRandomLabel).markFoundOccurrences(searchTerms),
    speakers,
    language.name.lowercase(), addedAt,
    description(convertRandomLabel)?.markFoundOccurrences(searchTerms),
    topic,
    video,
    if (video?.startsWith("https://vimeo.com/") == true) video.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null,
    "rooms.${room?.name?.lowercase()}",
    start?.formatTalkTime(lang),
    end?.formatTalkTime(lang),
    start?.formatTalkDate(lang),
    favorite,
    photoUrls
)

fun Talk.summary(convertRandomLabel: Boolean): String {
    if (event == "2019" && convertRandomLabel) {
        return when (format) {
            TalkFormat.RANDOM -> {
                if (language == Language.ENGLISH)
                    "This is a \"Random\" talk. For this track we choose the program for you. You are in a room, and a speaker comes to speak about a subject for which you ignore the content. Don't be afraid it's only for 20 minutes. As it's a surprise we don't display the session summary before...   "
                else
                    "Ce talk est de type \"random\". Pour cette track, nous choisissons le programme pour vous. Vous êtes dans une pièce et un speaker vient parler d'un sujet dont vous ignorez le contenu. N'ayez pas peur, c'est seulement pour 20 minutes. Comme c'est une surprise, nous n'affichons pas le résumé de la session avant ..."
            }
            TalkFormat.KEYNOTE_SURPRISE, TalkFormat.CLOSING_SESSION -> {
                if (language == Language.ENGLISH)
                    "This is a \"surprise\" talk. For our keynote we choose the programm for you. You are in a room, and a speaker come to speak about a subject for which you ignore the content. Don't be afraid it's only for 30 minutes. As it's a surprise we don't display the session summary before...   "
                else
                    "Ce talk est une \"surprise\". Pour cette track, nous choisissons le programme pour vous. Vous êtes dans une pièce et un speaker vient parler d'un sujet dont vous ignorez le contenu. N'ayez pas peur, c'est seulement pour 30 minutes. Comme c'est une surprise, nous n'affichons pas le résumé de la session avant ..."
            }
            else -> summary
        }
    }
    return summary
}

fun Talk.title(convertRandomLabel: Boolean, searchTerms: List<String> = emptyList()): String = if (convertRandomLabel && (format == TalkFormat.KEYNOTE_SURPRISE || format == TalkFormat.CLOSING_SESSION) && event == "2019") "A surprise keynote... is a surprise"
else title.markFoundOccurrences(searchTerms)

fun Talk.description(convertRandomLabel: Boolean) = if (convertRandomLabel && (format == TalkFormat.RANDOM || format == TalkFormat.KEYNOTE_SURPRISE || format == TalkFormat.CLOSING_SESSION) && event == "2019") "" else description

fun Talk.sanitizeForApi() = Talk(
    format, event, title(SURPRISE_RANDOM), summary(SURPRISE_RANDOM), speakerIds, language, addedAt, description(
        SURPRISE_RANDOM
    ), topic, video, room, start, end, photoUrls, slug, id
)