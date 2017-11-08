package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime


@Component
class TalkHandler(private val repository: TalkRepository,
                  private val userRepository: UserRepository,
                  private val eventRepository: EventRepository,
                  private val properties: MixitProperties,
                  private val markdownConverter: MarkdownConverter) {

    fun findByEventView(year: Int, req: ServerRequest, topic: String? = null): Mono<ServerResponse> {

        val talks = repository
                .findByEvent(year.toString(), topic)
                .collectList()
                .flatMap { talks ->
                    userRepository
                            .findMany(talks.flatMap(Talk::speakerIds))
                            .collectMap(User::login)
                            .map { speakers -> talks.map { it.toDto(req.language(), it.speakerIds.mapNotNull { speakers[it] }) }.groupBy { it.date } }
                }

        val sponsors = eventSponsors(year, req)

        return ok().render("talks", mapOf(
                Pair("talks", talks),
                Pair("year", year),
                Pair("title", when (topic) { null -> "talks.title.html|$year"
                    else -> "talks.title.html.$topic|$year"
                }),
                Pair("baseUri", UriUtils.encode(properties.baseUri!!, StandardCharsets.UTF_8)),
                Pair("topic", topic),
                Pair("has2Columns", talks.map { it.size == 2 }),
                Pair("sponsors", sponsors)
        ))
    }

    fun findOneView(year: Int, req: ServerRequest) = repository.findByEventAndSlug(year.toString(), req.pathVariable("slug")).flatMap { talk ->
        val sponsors = eventSponsors(year, req)

        userRepository.findMany(talk.speakerIds).collectList().flatMap { speakers ->
            ok().render("talk", mapOf(
                    Pair("talk", talk.toDto(req.language(), speakers!!)),
                    Pair("speakers", speakers.map { it.toDto(req.language(), markdownConverter) }.sortedBy { talk.speakerIds.indexOf(it.login) }),
                    Pair("title", "talk.html.title|${talk.title}"),
                    Pair("baseUri", UriUtils.encode(properties.baseUri!!, StandardCharsets.UTF_8)),
                    Pair("vimeoPlayer", if (talk.video?.startsWith("https://vimeo.com/") == true) talk.video.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null),
                    Pair("sponsors", sponsors)
            ))
        }
    }

    private fun eventSponsors(year: Int, req: ServerRequest) = eventRepository
            .findByYear(year)
            .flatMap { event ->
                userRepository
                        .findMany(event.sponsors.map { it.sponsorId })
                        .collectMap(User::login)
                        .map { sponsorsByLogin ->
                            val sponsorsByEvent = event.sponsors.groupBy { it.level }
                            mapOf(
                                    Pair("sponsors-gold", sponsorsByEvent[SponsorshipLevel.GOLD]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-silver", sponsorsByEvent[SponsorshipLevel.SILVER]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-hosting", sponsorsByEvent[SponsorshipLevel.HOSTING]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-lanyard", sponsorsByEvent[SponsorshipLevel.LANYARD]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-mixteen", sponsorsByEvent[SponsorshipLevel.MIXTEEN]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-party", sponsorsByEvent[SponsorshipLevel.PARTY]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-video", sponsorsByEvent[SponsorshipLevel.VIDEO]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) })
                            )
                        }
            }

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")))

    fun findByEventId(req: ServerRequest) = ok().json().body(repository.findByEvent(req.pathVariable("year")))

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
        video, "rooms.${room?.name?.toLowerCase()}", start?.formatTalkTime(lang), end?.formatTalkTime(lang),
        start?.formatTalkDate(lang)
)
