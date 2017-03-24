package mixit.web.handler

import mixit.model.*
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.json
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import java.time.LocalDateTime


@Component
class TalkHandler(val repository: TalkRepository,
                  val userRepository: UserRepository,
                  val markdownConverter: MarkdownConverter) {

    fun findByEventView(year: Int, req: ServerRequest) =
            repository.findByEvent(yearToId(year.toString())).collectList().then { sessions ->
                userRepository.findMany(sessions.flatMap(Talk::speakerIds)).collectMap(User::login).then { speakers ->
                val model = mapOf(Pair("talks", sessions.map { it.toDto(it.speakerIds.mapNotNull { speakers[it] } , markdownConverter) }), Pair("year", year), Pair("title", "talks.html.title|$year"))
                ok().render("talks", model)
            }}

    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug")).then { session ->
        userRepository.findMany(session.speakerIds).collectList().then { speakers ->
        ok().render("talk", mapOf(Pair("talk", session.toDto(speakers!!, markdownConverter)), Pair("speakers", speakers.map { it.toDto(req.language(), markdownConverter) }), Pair("title", "talk.html.title|${session.title}")))
    }}

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findByEventId(req: ServerRequest) =
            ok().json().body(repository.findByEvent(yearToId(req.pathVariable("year"))))

}

fun yearToId(year:String): String = "mixit${year.substring(2)}"

class TalkDto(
        val id: String?,
        val slug: String,
        val format: SessionFormat,
        val event: String,
        val title: String,
        val summary: String,
        val speakers: List<User>,
        val language: Language,
        val addedAt: LocalDateTime,
        val description: String?,
        val video: String?,
        val room: Room?,
        val start: LocalDateTime?,
        val end: LocalDateTime?
)

fun Talk.toDto(speakers: List<User>, markdownConverter: MarkdownConverter) = TalkDto(
        id, slug, format, event, title,
        markdownConverter.toHTML(summary), speakers, language, addedAt,
        markdownConverter.toHTML(description),
        video, room, start, end
)
