package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.*
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime


@Component
class TalkHandler(private val repository: TalkRepository,
                  private val userRepository: UserRepository,
                  private val properties: MixitProperties,
                  private val markdownConverter: MarkdownConverter) {

    fun findByEventView(year: Int, req: ServerRequest, topic: String? = null): Mono<ServerResponse> {
        val talks = repository
                .findByEvent(year.toString(), topic)
                .collectList()
                .flatMap { talks -> userRepository
                        .findMany(talks.flatMap(Talk::speakerIds))
                        .collectMap(User::login)
                        .map { speakers -> talks.map { it.toDto(req.language(), it.speakerIds.mapNotNull { speakers[it] }) }.groupBy { it.date } }
                }

        return ok().render("talks", mapOf(
                Pair("talks", talks),
                Pair("year", year),
                Pair("title", when(topic) { null -> "talks.title.html|$year" else -> "talks.title.html.$topic|$year" }),
                Pair("baseUri", UriUtils.encode(properties.baseUri!!, StandardCharsets.UTF_8)),
                Pair("topic", topic),
                Pair("has2Columns", talks.map { it.size == 2 })
        ))
    }


    fun findOneView(year: Int, req: ServerRequest) = repository.findByEventAndSlug(year.toString(), req.pathVariable("slug")).flatMap { talk ->
        userRepository.findMany(talk.speakerIds).collectList().flatMap { speakers ->
        ok().render("talk", mapOf(
                Pair("talk", talk.toDto(req.language(), speakers!!)),
                Pair("speakers", speakers.map { it.toDto(req.language(), markdownConverter) }.sortedBy { talk.speakerIds.indexOf(it.login) }),
                Pair("title", "talk.html.title|${talk.title}"),
                Pair("baseUri", UriUtils.encode(properties.baseUri!!, StandardCharsets.UTF_8)),
                Pair("vimeoPlayer", if(talk.video?.startsWith("https://vimeo.com/") == true) talk.video.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null)
        ))
    }}

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findByEventId(req: ServerRequest) =
            ok().json().body(repository.findByEvent(req.pathVariable("year")))

    fun redirectFromId(req: ServerRequest) = repository.findOne(req.pathVariable("id")).flatMap {
        permanentRedirect("${properties.baseUri}/${it.event}/${it.slug}")
    }

    fun redirectFromSlug(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug")).flatMap {
        permanentRedirect("${properties.baseUri}/${it.event}/${it.slug}")
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
        val date: String?,
        val isEn: Boolean = (language == "english"),
        val isTalk: Boolean = (format == TalkFormat.TALK)
)

fun Talk.toDto(lang: Language, speakers: List<User>) = TalkDto(
        id, slug, format, event, title,
        summary, speakers, language.name.toLowerCase(), addedAt,
        description, topic,
        video, "rooms.${room?.name?.toLowerCase()}" , start?.formatTalkTime(lang), end?.formatTalkTime(lang),
        start?.formatTalkDate(lang)
)
