package mixit.web.handler.user

import java.nio.charset.StandardCharsets
import mixit.MixitProperties
import mixit.model.Favorite
import mixit.model.Language
import mixit.model.SponsorshipLevel
import mixit.model.Talk
import mixit.model.User
import mixit.repository.EventRepository
import mixit.repository.FavoriteRepository
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.extractFormData
import mixit.util.json
import mixit.util.language
import mixit.util.permanentRedirect
import mixit.util.seeOther
import mixit.util.validator.MarkdownValidator
import mixit.util.validator.MaxLengthValidator
import mixit.web.handler.talk.TalkDto
import mixit.web.handler.talk.sanitizeForApi
import mixit.web.handler.talk.toDto
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

const val SURPRISE_RANDOM = false

@Component
class TalkHandler(
    private val repository: TalkRepository,
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val properties: MixitProperties,
    private val markdownConverter: MarkdownConverter,
    private val favoriteRepository: FavoriteRepository,
    private val maxLengthValidator: MaxLengthValidator,
    private val markdownValidator: MarkdownValidator
) {

    fun findByEventView(year: Int, req: ServerRequest, filterOnFavorite: Boolean, topic: String? = null): Mono<ServerResponse> =
        req.session().flatMap { session ->
            val currentUserEmail = session.getAttribute<String>("email")
            eventRepository
                .findByYear(year)
                .flatMap { event ->
                    val talks = loadTalkAndFavorites(year, req.language(), filterOnFavorite, currentUserEmail, topic)
                        .map { talk -> talk.groupBy { it.date ?: "" } }

                    val sponsors = loadSponsors(year, req)
                    ok().render(
                        "talks",
                        mapOf(
                            Pair("talks", talks),
                            Pair("year", year),
                            Pair("schedulingFile", event.schedulingFileUrl),
                            Pair("current", year == 2019),
                            Pair(
                                "title",
                                if(topic == null) "talks.title.html|$year" else "talks.title.html.$topic|$year"
                            ),
                            Pair("filtered", filterOnFavorite),
                            Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                            Pair("topic", topic),
                            Pair("has2Columns", talks.map { it.size == 2 }),
                            Pair("sponsors", sponsors)
                        )
                    )
                }
        }

    fun findMediaByEventView(year: Int, req: ServerRequest, filterOnFavorite: Boolean, topic: String? = null): Mono<ServerResponse> =
        req.session().flatMap {
            val currentUserEmail = it.getAttribute<String>("email")
            val talks = loadTalkAndFavorites(year, req.language(), filterOnFavorite, currentUserEmail, topic).map { it.sortedBy { it.title } }
            val sponsors = loadSponsors(year, req)

            eventRepository
                .findByYear(year)
                .flatMap { event ->

                    ok().render(
                        "medias",
                        mapOf(
                            Pair("talks", talks),
                            Pair("topic", topic),
                            Pair("year", year),
                            Pair("title", "medias.title.html|$year"),
                            Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                            Pair("sponsors", sponsors),
                            Pair("filtered", filterOnFavorite),
                            Pair("event", event),
                            Pair("videoUrl", if (event.videoUrl?.url?.startsWith("https://vimeo.com/") == true) event.videoUrl.url.replace("https://vimeo.com/", "https://player.vimeo.com/video/") else null),
                            Pair("hasPhotosOrVideo", event.videoUrl != null || event.photoUrls.isNotEmpty())
                        )
                    )
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

    private fun addUserToTalks(
        talks: List<Talk>,
        favorites: List<Favorite>,
        language: Language
    ): Mono<List<TalkDto>> =
        userRepository
            .findMany(talks.flatMap(Talk::speakerIds))
            .collectMap(User::login)
            .map { speakers ->
                talks
                    .map { talk ->
                        talk.toDto(
                            language,
                            talk.speakerIds.mapNotNull { speakers[it] },
                            favorites.filter { talk.id!!.equals(it.talkId) }.any()
                        )
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

                ok().render(
                    "talk",
                    mapOf(
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
                    )
                )
            }
        }
    }

    fun editTalkView(req: ServerRequest) =
        repository
            .findBySlug(req.pathVariable("slug"))
            .flatMap { editTalkViewDetail(req, it, emptyMap()) }

    private fun editTalkViewDetail(req: ServerRequest, talk: Talk, errors: Map<String, String>) = userRepository.findMany(talk.speakerIds).collectList().flatMap { speakers ->

        ok().render(
            "talk-edit",
            mapOf(
                Pair("talk", talk.toDto(req.language(), speakers!!, convertRandomLabel = false)),
                Pair("speakers", speakers.map { speaker -> speaker.toDto(req.language(), markdownConverter) }.sortedBy { talk.speakerIds.indexOf(it.login) }),
                Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                Pair("hasErrors", errors.isNotEmpty()),
                Pair("errors", errors),
                Pair(
                    "languages",
                    listOf(
                        Pair(Language.ENGLISH, Language.ENGLISH == talk.language),
                        Pair(Language.FRENCH, Language.FRENCH == talk.language)
                    )
                )
            )
        )
    }

    fun saveProfileTalk(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
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
                        Pair(
                            "sponsors-others",
                            event.sponsors
                                .filter { it.level != SponsorshipLevel.GOLD }
                                .map { it.toSponsorDto(sponsorsByLogin[it.sponsorId]!!) }
                                .distinctBy { it.login }
                        )
                    )
                }
        }

    fun findOne(req: ServerRequest) =
        ok().json().body(repository.findOne(req.pathVariable("login")).map { it.sanitizeForApi() })

    fun findByEventId(req: ServerRequest) =
        ok().json().body(repository.findByEvent(req.pathVariable("year")).map { it.sanitizeForApi() })

    fun findAdminByEventId(req: ServerRequest) =
        ok().json().body(repository.findByEvent(req.pathVariable("year")))

    fun redirectFromId(req: ServerRequest) = repository.findOne(req.pathVariable("id")).flatMap {
        permanentRedirect("${properties.baseUri}/${it.event}/${it.slug}")
    }

    fun redirectFromSlug(req: ServerRequest) = repository.findBySlug(req.pathVariable("slug")).flatMap {
        permanentRedirect("${properties.baseUri}/${it.event}/${it.slug}")
    }
}

