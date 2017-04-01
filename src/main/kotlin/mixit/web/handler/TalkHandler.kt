package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import java.time.LocalDateTime


@Component
class TalkHandler(val repository: TalkRepository,
                  val userRepository: UserRepository,
                  val markdownConverter: MarkdownConverter,
                  val properties: MixitProperties) {

    fun findByEventView(year: Int, req: ServerRequest, topic: String? = null) = ok().render("talks", mapOf(
            Pair("talks", repository
                    .findByEvent(year.toString(), topic)
                    .collectList()
                    .then { talks -> userRepository
                            .findMany(talks.flatMap(Talk::speakerIds))
                            .collectMap(User::login)
                            .map { speakers -> talks.map { it.toDto(req.language(), it.speakerIds.mapNotNull { speakers[it] }, markdownConverter) } }
                    }),
            Pair("year", year),
            Pair("title", "talks.html.title|$year")
    ))



    fun findOneView(year: Int, req: ServerRequest) = repository.findByEventAndSlug(year.toString(), req.pathVariable("slug")).then { talk ->
        userRepository.findMany(talk.speakerIds).collectList().then { speakers ->
        ok().render("talk", mapOf(Pair("talk", talk.toDto(req.language(), speakers!!, markdownConverter)), Pair("speakers", speakers.map { it.toDto(req.language(), markdownConverter) }), Pair("title", "talk.html.title|${talk.title}")))
    }}

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findByEventId(req: ServerRequest) =
            ok().json().body(repository.findByEvent(req.pathVariable("year")))

    fun redirectFromId(req: ServerRequest) = repository.findOne(req.pathVariable("id")).then { s ->
        permanentRedirect("${properties.baseUri}/${s.event}/${s.slug}")
    }

    fun redirectFromSlug(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug")).then { s ->
        permanentRedirect("${properties.baseUri}/${s.event}/${s.slug}")
    }

}

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
        val room: String?,
        val start: String?,
        val end: String?,
        val date: String?
)

fun Talk.toDto(lang: Language, speakers: List<User>, markdownConverter: MarkdownConverter) = TalkDto(
        id, slug, format, event, title,
        markdownConverter.toHTML(summary), speakers, language.name.toLowerCase(), addedAt,
        markdownConverter.toHTML(description), topic,
        video, "rooms.${room?.name?.toLowerCase()}" , start?.formatTalkTime(lang), end?.formatTalkTime(lang),
        start?.formatTalkDate(lang)
)
