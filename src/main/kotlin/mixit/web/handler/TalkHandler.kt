package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.FavoriteRepository
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.*
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime


@Component
class TalkHandler(private val repository: TalkRepository,
                  private val userRepository: UserRepository,
                  private val eventRepository: EventRepository,
                  private val properties: MixitProperties,
                  private val markdownConverter: MarkdownConverter,
                  private val favoriteRepository: FavoriteRepository) {

    fun findByEventView(year: Int, req: ServerRequest, topic: String? = null): Mono<ServerResponse> =
            req.session().flatMap {
                val currentUserEmail = it.getAttribute<String>("email")

                val talks = repository
                        .findByEvent(year.toString(), topic)
                        .collectList()
                        .flatMap { talks ->
                            if (currentUserEmail != null) {
                                val favorites = favoriteRepository
                                        .findByEmailAndTalks(currentUserEmail, talks.map { it.id!! })
                                        .collectMap(Favorite::talkId)
                                addUserToTalk(talks, favorites.block(), req.language())
                            } else {
                                addUserToTalk(talks, emptyMap(), req.language())
                            }
                        }

                val sponsors = eventSponsors(year, req)

                ok().render("talks", mapOf(
                        Pair("talks", talks),
                        Pair("year", year),
                        Pair("current", year == 2018),
                        Pair("title", when (topic) { null -> "talks.title.html|$year"
                            else -> "talks.title.html.$topic|$year"
                        }),
                        Pair("baseUri", UriUtils.encode(properties.baseUri!!, StandardCharsets.UTF_8)),
                        Pair("topic", topic),
                        Pair("has2Columns", talks.map { it.size == 2 }),
                        Pair("sponsors", sponsors)
                ))
            }

    private fun addUserToTalk(talks: List<Talk>,
                              favorites: Map<String, Favorite>? = emptyMap(),
                              language: Language): Mono<Map<String, List<TalkDto>>> = userRepository
            .findMany(talks.flatMap(Talk::speakerIds))
            .collectMap(User::login)
            .map { speakers ->
                talks.map { it.toDto(language, it.speakerIds.mapNotNull { speakers[it] }, favorites!!.get(it.id) != null) }.groupBy { if (it.date == null) "" else it.date }
            }

    private fun addUserToTalkForMedia(talks: List<Talk>,
                              favorites: Map<String, Favorite>? = emptyMap(),
                              language: Language) = userRepository
            .findMany(talks.flatMap(Talk::speakerIds))
            .collectMap(User::login)
            .map { speakers ->
                talks.sortedBy { it.title }.map { it.toDto(language, it.speakerIds.mapNotNull { speakers[it] }, favorites!!.get(it.id) != null) }
            }


    fun findOneView(year: Int, req: ServerRequest): Mono<ServerResponse> = repository.findByEventAndSlug(year.toString(), req.pathVariable("slug")).flatMap { talk ->
        val sponsors = eventSponsors(year, req)

        req.session().flatMap {
            val currentUserEmail = it.getAttribute<String>("email")

            userRepository.findMany(talk.speakerIds).collectList().flatMap { speakers ->

                val otherTalks = repository
                        .findBySpeakerId(talk.speakerIds, talk.id)
                        .collectList()
                        .flatMap { talks ->
                            talks.map { talk -> talk.toDto(req.language(), speakers.filter { talk.speakerIds.contains(it.login) }.toList()) }.toMono()
                        }

                ok().render("talk", mapOf(
                        Pair("talk", talk.toDto(req.language(), speakers!!)),
                        Pair("speakers", speakers.map { speaker -> speaker.toDto(req.language(), markdownConverter) }.sortedBy { talk.speakerIds.indexOf(it.login) }),
                        Pair("othertalks", otherTalks),
                        Pair("favorites", if (currentUserEmail == null) null else favoriteRepository.findByEmailAndTalk(currentUserEmail, talk.id!!)),
                        Pair("year", year),
                        Pair("hasOthertalks", otherTalks.map { it.size > 0 }),
                        Pair("title", "talk.html.title|${talk.title}"),
                        Pair("baseUri", UriUtils.encode(properties.baseUri!!, StandardCharsets.UTF_8)),
                        Pair("vimeoPlayer", if (talk.video?.startsWith("https://vimeo.com/") == true) talk.video.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null),
                        Pair("sponsors", sponsors)
                ))
            }
        }
    }

    fun findMediaTopicByEventView(year: Int, req: ServerRequest): Mono<ServerResponse> = findMediaByEventView(year, req, req.pathVariable("topic"))

    fun findMediaByEventView(year: Int, req: ServerRequest, topic: String? = null): Mono<ServerResponse> = eventRepository.findByYear(year).flatMap { event ->
        req.session().flatMap {
            val currentUserEmail = it.getAttribute<String>("email")

            val talks = repository
                    .findByEvent(year.toString(), topic)
                    .filter { !StringUtils.isEmpty(it.video) }
                    .collectList()
                    .flatMap { talks ->
                        if (currentUserEmail != null) {
                            val favorites = favoriteRepository
                                    .findByEmailAndTalks(currentUserEmail, talks.map { it.id!! })
                                    .collectMap(Favorite::talkId)
                            addUserToTalkForMedia(talks, favorites.block(), req.language())
                        } else {
                            addUserToTalkForMedia(talks, emptyMap(), req.language())
                        }
                    }

            val sponsors = eventSponsors(year, req)

            ok().render("medias", mapOf(
                    Pair("talks", talks),
                    Pair("topic", topic),
                    Pair("year", year),
                    Pair("title", "medias.title.html|$year"),
                    Pair("baseUri", UriUtils.encode(properties.baseUri!!, StandardCharsets.UTF_8)),
                    Pair("sponsors", sponsors),
                    Pair("event", event),
                    Pair("videoUrl", if (event.videoUrl?.url?.startsWith("https://vimeo.com/") == true) event.videoUrl.url.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null),
                    Pair("hasPhotosOrVideo", event.videoUrl != null || event.photoUrls.isNotEmpty())
            ))
        }
    }

    fun eventSponsors(year: Int, req: ServerRequest) = eventRepository
            .findByYear(year)
            .flatMap { event ->
                userRepository
                        .findMany(event.sponsors.map { it.sponsorId })
                        .collectMap(User::login)
                        .map { sponsorsByLogin ->
                            val sponsorsByEvent = event.sponsors.groupBy { it.level }
                            mapOf(
                                    Pair("sponsors-gold", sponsorsByEvent[SponsorshipLevel.GOLD]?.map { it.toSponsorDto(sponsorsByLogin[it.sponsorId]!!) }),
                                    Pair("sponsors-others", event.sponsors
                                            .filter { it.level != SponsorshipLevel.GOLD }
                                            .map { it.toSponsorDto(sponsorsByLogin[it.sponsorId]!!) }
                                            .distinctBy { it.login })
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
        val vimeoPlayer: String?,
        val room: String?,
        val start: String?,
        val end: String?,
        val date: String?,
        val favorite: Boolean = false,
        val photoUrls: List<Link> = emptyList(),
        val isEn: Boolean = (language == "english"),
        val isTalk: Boolean = (format == TalkFormat.TALK),
        val multiSpeaker: Boolean = (speakers.size > 1),
        val speakersFirstNames: String = (speakers.joinToString { it.firstname })
)

fun Talk.toDto(lang: Language, speakers: List<User>, favorite: Boolean = false) = TalkDto(
        id, slug, format, event, title,
        summary(),
        speakers, language.name.toLowerCase(), addedAt,
        description(),
        topic,
        video,
        if (video?.startsWith("https://vimeo.com/") == true) video.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null,
        "rooms.${room?.name?.toLowerCase()}",
        start?.formatTalkTime(lang),
        end?.formatTalkTime(lang),
        start?.formatTalkDate(lang),
        favorite,
        photoUrls
)

fun Talk.summary() = if(format == TalkFormat.RANDOM  && language == Language.ENGLISH && event=="2018")
    "This is This is a \"Random\" talk. For this track we choose the programm for you. You are in a room, and a speaker come to speak about a subject for which you ignore the content. Don't be afraid it's only for 25 minutes. As it's a surprise we don't display the session summary before...   "
    else if(format == TalkFormat.RANDOM  && language == Language.FRENCH && event=="2018")
    "Ce talk est de type \"random\". Pour cette track, nous choisissons le programme pour vous. Vous êtes dans une pièce et un speaker vient parler d'un sujet dont vous ignorez le contenu. N'ayez pas peur, c'est seulement pour 25 minutes. Comme c'est une surprise, nous n'affichons pas le résumé de la session avant ..."
    else summary

fun Talk.description() = if(format == TalkFormat.RANDOM && event=="2018") "" else description
