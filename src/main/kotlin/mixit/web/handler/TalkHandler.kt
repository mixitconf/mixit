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

    fun findByEventView(year: Int, req: ServerRequest) = ok().render("talks", mapOf(
            Pair("talks", repository
                    .findByEvent(yearToId(year.toString()))
                    .collectList()
                    .then { talks -> userRepository
                            .findMany(talks.flatMap(Talk::speakerIds))
                            .collectMap(User::login)
                            .map { speakers -> talks.map { it.toDto(req.language(), it.speakerIds.mapNotNull { speakers[it] }, markdownConverter) } }
                    }),
            Pair("year", year),
            Pair("title", "talks.html.title|$year")
    ))



    fun findOneView(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug")).then { talk ->
        userRepository.findMany(talk.speakerIds).collectList().then { speakers ->
        ok().render("talk", mapOf(Pair("talk", talk.toDto(req.language(), speakers!!, markdownConverter)), Pair("speakers", speakers.map { it.toDto(req.language(), markdownConverter) }), Pair("title", "talk.html.title|${talk.title}")))
    }}

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findByEventId(req: ServerRequest) =
            ok().json().body(repository.findByEvent(yearToId(req.pathVariable("year"))))

    fun redirect(req: ServerRequest) = repository.findOne(req.pathVariable("id")).then { s ->
        permanentRedirect("${properties.baseUri}/talk/${s.slug}")
    }

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
        val topic: String?,
        val video: String?,
        val room: String?,
        val start: String?,
        val end: String?,
        val date: String?
)

fun Talk.toDto(language: Language, speakers: List<User>, markdownConverter: MarkdownConverter) = TalkDto(
        id, slug, format, event, title,
        markdownConverter.toHTML(summary), speakers, language, addedAt,
        markdownConverter.toHTML(description), topic,
        video, "rooms.${room?.name?.toLowerCase()}" , start?.formatTalkTime(language), end?.formatTalkTime(language),
        start?.formatTalkDate(language)
)
