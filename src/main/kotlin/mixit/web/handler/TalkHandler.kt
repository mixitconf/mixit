package mixit.web.handler

import mixit.model.*
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.context.MessageSource
import org.springframework.context.support.AbstractMessageSource
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import java.time.LocalDateTime


@Component
class TalkHandler(val repository: TalkRepository,
                  val userRepository: UserRepository,
                  val markdownConverter: MarkdownConverter) {

    fun findByEventView(year: Int, req: ServerRequest) =
            repository.findByEvent(yearToId(year.toString())).collectList().then { talks ->
                userRepository.findMany(talks.flatMap(Talk::speakerIds)).collectMap(User::login).then { speakers ->
                val model = mapOf(Pair("talks", talks.map { it.toDto(req.language(), it.speakerIds.mapNotNull { speakers[it] } , markdownConverter) }), Pair("year", year), Pair("title", "talks.html.title|$year"))
                ok().render("talks", model)
            }}

    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug")).then { session ->
        userRepository.findMany(session.speakerIds).collectList().then { speakers ->
        ok().render("talk", mapOf(Pair("talk", session.toDto(req.language(), speakers!!, markdownConverter)), Pair("speakers", speakers.map { it.toDto(req.language(), markdownConverter) }), Pair("title", "talk.html.title|${session.title}")))
    }}

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findByEventId(req: ServerRequest) =
            ok().json().body(repository.findByEvent(yearToId(req.pathVariable("year"))))

}

fun yearToId(year:String): String = "mixit${year.substring(2)}"

class TalkDto(
        val id: String?,
        val slug: String,
        val format: TalkFormat,
        val event: String,
        val title: String,
        val summary: String,
        val speakers: List<User>,
        val language: Language,
        val addedAt: LocalDateTime,
        val description: String?,
        val video: String?,
        val room: String?,
        val start: String?,
        val end: String?,
        val date: String?
)

fun Talk.toDto(language: Language, speakers: List<User>, markdownConverter: MarkdownConverter) = TalkDto(
        id, slug, format, event, title,
        markdownConverter.toHTML(summary), speakers, language, addedAt,
        markdownConverter.toHTML(description),
        video, "rooms.${room?.name?.toLowerCase()}" , start?.formatTalkTime(language), end?.formatTalkTime(language),
        start?.formatTalkDate(language)
)
