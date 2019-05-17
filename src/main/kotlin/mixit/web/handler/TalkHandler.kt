package mixit.web.handler

import mixit.MixitProperties
import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.FavoriteRepository
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.*
import mixit.util.validator.MarkdownValidator
import mixit.util.validator.MaxLengthValidator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
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
                  private val favoriteRepository: FavoriteRepository,
                  private val maxLengthValidator: MaxLengthValidator,
                  private val markdownValidator: MarkdownValidator) {


    fun findByEventView(year: Int, req: ServerRequest, filterOnFavorite: Boolean, topic: String? = null): Mono<ServerResponse> =
            req.session().flatMap {
                val currentUserEmail = it.getAttribute<String>("email")
                val talks = loadTalkAndFavorites(year, req.language(), filterOnFavorite, currentUserEmail, topic)
                        .map { it.groupBy { if (it.date == null) "" else it.date } }

                val sponsors = loadSponsors(year, req)

                ok().render("talks", mapOf(
                        Pair("talks", talks),
                        Pair("year", year),
                        Pair("schedulingFile", getSchedulingFile(year)),
                        Pair("current", year == 2019),
                        Pair("title", when (topic) {
                            null -> "talks.title.html|$year"
                            else -> "talks.title.html.$topic|$year"
                        }),
                        Pair("filtered", filterOnFavorite),
                        Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                        Pair("topic", topic),
                        Pair("has2Columns", talks.map { it.size == 2 }),
                        Pair("sponsors", sponsors)
                ))
            }

    fun findMediaByEventView(year: Int, req: ServerRequest, filterOnFavorite: Boolean, topic: String? = null): Mono<ServerResponse> =
            req.session().flatMap {
                val currentUserEmail = it.getAttribute<String>("email")
                val talks = loadTalkAndFavorites(year, req.language(), filterOnFavorite, currentUserEmail, topic).map { it.sortedBy { it.title } }
                val sponsors = loadSponsors(year, req)

                eventRepository
                        .findByYear(year)
                        .flatMap { event ->

                            ok().render("medias", mapOf(
                                    Pair("talks", talks),
                                    Pair("topic", topic),
                                    Pair("year", year),
                                    Pair("title", "medias.title.html|$year"),
                                    Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                                    Pair("sponsors", sponsors),
                                    Pair("filtered", filterOnFavorite),
                                    Pair("event", event),
                                    Pair("videoUrl", if (event.videoUrl?.url?.startsWith("https://vimeo.com/") == true) event.videoUrl.url.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null),
                                    Pair("hasPhotosOrVideo", event.videoUrl != null || event.photoUrls.isNotEmpty())))
                        }

            }

    private fun loadTalkAndFavorites(year: Int, language: Language, filterOnFavorite: Boolean, currentUserEmail: String? = null, topic: String? = null): Mono<List<TalkDto>> =
            if (currentUserEmail != null) {
                favoriteRepository
                        .findByEmail(currentUserEmail)
                        .collectList()
                        .flatMap { favorites ->
                            if (filterOnFavorite) {
                                repository.findByEventAndTalkIds(year.toString(), favorites.map { it.talkId }, topic)
                                        .collectList().flatMap { addUserToTalks(it, favorites, language) }
                            } else {
                                repository.findByEvent(year.toString(), topic).collectList().flatMap { addUserToTalks(it, favorites, language) }
                            }
                        }
            } else {
                repository.findByEvent(year.toString(), topic).collectList().flatMap { addUserToTalks(it, emptyList(), language) }
            }

    private fun addUserToTalks(talks: List<Talk>,
                               favorites: List<Favorite>,
                               language: Language): Mono<List<TalkDto>> =
            userRepository
                    .findMany(talks.flatMap(Talk::speakerIds))
                    .collectMap(User::login)
                    .map { speakers ->
                        talks
                                .map { talk ->
                                    talk.toDto(language,
                                            talk.speakerIds.mapNotNull { speakers[it] },
                                            favorites.filter { talk.id!!.equals(it.talkId) }.any())
                                }
                    }


    fun findOneView(year: Int, req: ServerRequest): Mono<ServerResponse> = repository.findByEventAndSlug(year.toString(), req.pathVariable("slug")).flatMap { talk ->
        val sponsors = loadSponsors(year, req)

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
                        Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                        Pair("vimeoPlayer", if (talk.video?.startsWith("https://vimeo.com/") == true) talk.video.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null),
                        Pair("sponsors", sponsors)
                ))
            }
        }
    }

    fun editTalkView(req: ServerRequest) =
            repository
                    .findBySlug(req.pathVariable("slug"))
                    .flatMap { editTalkViewDetail(req, it, emptyMap()) }

    private fun editTalkViewDetail(req: ServerRequest, talk: Talk, errors: Map<String, String>) = userRepository.findMany(talk.speakerIds).collectList().flatMap { speakers ->

        ok().render("talk-edit", mapOf(
                Pair("talk", talk.toDto(req.language(), speakers!!, convertRandomLabel = false)),
                Pair("speakers", speakers.map { speaker -> speaker.toDto(req.language(), markdownConverter) }.sortedBy { talk.speakerIds.indexOf(it.login) }),
                Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                Pair("hasErrors", errors.isNotEmpty()),
                Pair("errors", errors),
                Pair("languages", listOf(
                        Pair(Language.ENGLISH, Language.ENGLISH == talk.language),
                        Pair(Language.FRENCH, Language.FRENCH == talk.language)
                ))
        ))
    }

    fun saveProfileTalk(req: ServerRequest): Mono<ServerResponse> = req.body(BodyExtractors.toFormData()).flatMap {
        val formData = it.toSingleValueMap()

        repository.findOne(formData["id"]!!).flatMap {

            val errors = mutableMapOf<String, String>()

            // Null check
            if (formData["title"].isNullOrBlank()) {
                errors.put("title", "talk.form.error.title.required")
            }
            if (formData["summary"].isNullOrBlank()) {
                errors.put("title", "talk.form.error.summary.required")
            }
            if (formData["language"].isNullOrBlank()) {
                errors.put("title", "talk.form.error.language.required")
            }

            if (errors.isNotEmpty()) {
                editTalkViewDetail(req, it, errors)
            }

            val talk = Talk(
                    it.format,
                    it.event,
                    formData["title"]!!,
                    markdownValidator.sanitize(formData["summary"]!!),
                    it.speakerIds,
                    Language.valueOf(formData["language"]!!),
                    it.addedAt,
                    markdownValidator.sanitize(formData["description"]),
                    it.topic,
                    it.video,
                    it.room,
                    it.start,
                    it.end,
                    it.photoUrls,
                    id = it.id
            )

            // We want to control data to not save invalid things in our database
            if (!maxLengthValidator.isValid(talk.title, 255)) {
                errors.put("title", "talk.form.error.title.size")
            }

            if (!markdownValidator.isValid(talk.summary)) {
                errors.put("summary", "talk.form.error.summary")
            }

            if (!markdownValidator.isValid(talk.description)) {
                errors.put("description", "talk.form.error.description")
            }

            if (errors.isEmpty()) {
                // If everything is Ok we save the user
                repository.save(talk).then(seeOther("${properties.baseUri}/me"))
            } else {
                editTalkViewDetail(req, talk, errors)
            }
        }

    }


    fun loadSponsors(year: Int, req: ServerRequest) = eventRepository
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

    fun findOne(req: ServerRequest) = ok().json().body(repository.findOne(req.pathVariable("login")).map { it.sanitizeForApi() })

    fun findByEventId(req: ServerRequest) = ok().json().body(repository.findByEvent(req.pathVariable("year")).map { it.sanitizeForApi() })

    fun findAdminByEventId(req: ServerRequest) = ok().json().body(repository.findByEvent(req.pathVariable("year")))


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
        val isCurrentEdition: Boolean = "2019".equals(event),
        val multiSpeaker: Boolean = (speakers.size > 1),
        val speakersFirstNames: String = (speakers.joinToString { it.firstname })
)

fun Talk.toDto(lang: Language, speakers: List<User>, favorite: Boolean = false, convertRandomLabel: Boolean = true, searchTerms: List<String> = emptyList()) = TalkDto(
        id, slug, format, event,
        title(convertRandomLabel, searchTerms),
        summary(convertRandomLabel).markFoundOccurrences(searchTerms),
        speakers,
        language.name.toLowerCase(), addedAt,
        description(convertRandomLabel)?.markFoundOccurrences(searchTerms),
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

fun Talk.summary(convertRandomLabel: Boolean): String {
    if (event == "2019" && convertRandomLabel) {
        return when (format) {
            TalkFormat.RANDOM -> {
                if (language == Language.ENGLISH)
                    "This is a \"Random\" talk. For this track we choose the programm for you. You are in a room, and a speaker come to speak about a subject for which you ignore the content. Don't be afraid it's only for 20 minutes. As it's a surprise we don't display the session summary before...   "
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

fun Talk.sanitizeForApi() = Talk(format, event, title(true), summary(true), speakerIds, language, addedAt, description(true), topic, video, room, start, end, photoUrls, slug, id)

// TODO put these data in Event table and add element on admin page to update them
private fun getSchedulingFile(event: Int): String? = when (event) {
    2019 -> "/pdf/planning2019v3.pdf"
    2018 -> "/pdf/planning2018.pdf"
    2017 -> "/pdf/planning2017.pdf"
    else -> null
}