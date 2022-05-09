package mixit.talk.handler

import mixit.MixitProperties
import mixit.event.model.CachedEvent
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel
import mixit.favorite.model.Favorite
import mixit.favorite.repository.FavoriteRepository
import mixit.talk.model.CachedTalk
import mixit.talk.model.Language
import mixit.talk.model.TalkService
import mixit.user.handler.toDto
import mixit.user.handler.toSponsorDto
import mixit.util.currentNonEncryptedUserEmail
import mixit.util.enumMatcher
import mixit.util.extractFormData
import mixit.util.language
import mixit.util.permanentRedirect
import mixit.util.seeOther
import mixit.util.toVimeoPlayerUrl
import mixit.util.validator.MarkdownValidator
import mixit.util.validator.MaxLengthValidator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.util.UriUtils
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.nio.charset.StandardCharsets

@Component
class TalkHandler(
    private val service: TalkService,
    private val eventService: EventService,
    private val properties: MixitProperties,
    private val favoriteRepository: FavoriteRepository,
    private val maxLengthValidator: MaxLengthValidator,
    private val markdownValidator: MarkdownValidator
) {

    companion object {
        const val TEMPLATE_SCHEDULE = "schedule"
        const val TEMPLATE_LIST = "talks"
        const val TEMPLATE_MEDIAS = "medias"
        const val TEMPLATE_VIEW = "talk"
        const val TEMPLATE_EDIT = "talk-edit"
    }

    fun scheduleView(req: ServerRequest) =
        ok().render(TEMPLATE_SCHEDULE, mapOf(Pair("title", "schedule.title")))

    fun findByEventViewForFeedbackWall(year: Int, req: ServerRequest) =
        findByEventView(year, req, false, template= "talks-feedback-wall")

    fun findByEventView(
        year: Int,
        req: ServerRequest,
        filterOnFavorite: Boolean,
        topic: String? = null,
        template: String = TEMPLATE_LIST
    ): Mono<ServerResponse> =
        req.currentNonEncryptedUserEmail()
            .flatMap { currentUserEmail ->
                if (currentUserEmail.isNotEmpty())
                    loadTalkAndFavorites(year, req.language(), filterOnFavorite, currentUserEmail, topic)
                else
                    service.findByEvent(year.toString(), topic).map { talks -> talks.map { it.toDto(req.language()) } }
            }
            .flatMap { talks ->
                eventService.findByYear(year).flatMap { event ->
                    val title = if (topic == null) "talks.title.html|$year" else "talks.title.html.$topic|$year"
                    ok().render(
                        template,
                        mapOf(
                            Pair("talks", talks.groupBy { it.date ?: "" }),
                            Pair("year", year),
                            Pair("schedulingFileUrl", event.schedulingFileUrl),
                            Pair("title", title),
                            Pair("filtered", filterOnFavorite),
                            Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                            Pair("topic", topic),
                            Pair("sponsors", loadSponsors(event))
                        )
                    )
                }
            }

    fun findMediaByEventView(
        year: Int,
        req: ServerRequest,
        filterOnFavorite: Boolean,
        topic: String? = null
    ): Mono<ServerResponse> =
        req.currentNonEncryptedUserEmail()
            .flatMap { currentUserEmail ->
                loadTalkAndFavorites(year, req.language(), filterOnFavorite, currentUserEmail, topic)
            }
            .switchIfEmpty {
                service.findByEvent(year.toString(), topic).map { talks -> talks.map { it.toDto(req.language()) } }
            }
            .flatMap { talks ->
                eventService.findByYear(year).flatMap { event ->
                    ok().render(
                        TEMPLATE_MEDIAS,
                        mapOf(
                            Pair("talks", talks.sortedBy { it.title }),
                            Pair("topic", topic),
                            Pair("year", year),
                            Pair("title", "medias.title.html|$year"),
                            Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                            Pair("sponsors", loadSponsors(event)),
                            Pair("filtered", filterOnFavorite),
                            Pair("event", event.toEvent()),
                            Pair("videoUrl", event.videoUrl?.url?.toVimeoPlayerUrl()),
                            Pair("hasPhotosOrVideo", event.videoUrl != null || event.photoUrls.isNotEmpty())
                        )
                    )
                }
            }

    private fun loadTalkAndFavorites(
        year: Int,
        language: Language,
        filterOnFavorite: Boolean,
        currentUserEmail: String,
        topic: String? = null
    ): Mono<List<TalkDto>> =
        favoriteRepository.findByEmail(currentUserEmail).collectList().flatMap { favorites ->
            val favoriteTalkIds = favorites.map { it.talkId }
            if (filterOnFavorite) {
                service.findByEventAndTalkIds(year.toString(), favoriteTalkIds, topic)
                    .map { talks ->
                        talks.map { it.toDto(language, favorite = true) }
                    }
            } else {
                service.findByEvent(year.toString(), topic)
                    .map { talks ->
                        talks.map { it.toDto(language, favorite = favoriteTalkIds.contains(it.id)) }
                    }
            }
        }

    fun findOneView(year: Int, req: ServerRequest): Mono<ServerResponse> =
        req.currentNonEncryptedUserEmail()
            .flatMap { currentUserEmail -> favoriteRepository.findByEmail(currentUserEmail).collectList() }
            .switchIfEmpty { Mono.just(emptyList<Favorite>()) }
            .flatMap { favorites ->
                eventService.findByYear(year).flatMap { event ->
                    service.findByEventAndSlug(year.toString(), req.pathVariable("slug")).flatMap { talk ->
                        val lang = req.language()
                        val inFavorite = favorites.any { talk.id == it.talkId }
                        val speakers = talk.speakers.map { it.toDto(lang) }.sortedBy { it.firstname }
                        val otherTalks = service.findBySpeakerId(speakers.map { it.login }, talk.id)
                            .map { talks -> talks.map { it.toDto(lang) } }

                        ok().render(
                            TEMPLATE_VIEW,
                            mapOf(
                                Pair("talk", talk.toDto(req.language())),
                                Pair("speakers", talk.speakers.map { it.toDto(lang) }.sortedBy { it.firstname }),
                                Pair("othertalks", otherTalks),
                                Pair("favorites", inFavorite),
                                Pair("year", year),
                                Pair("hasOthertalks", otherTalks.map { it.isNotEmpty() }),
                                Pair("title", "talk.html.title|${talk.title}"),
                                Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                                Pair("vimeoPlayer", talk.video.toVimeoPlayerUrl()),
                                Pair("sponsors", loadSponsors(event))
                            )
                        )
                    }
                }
            }

    fun editTalkView(req: ServerRequest) =
        service
            .findBySlug(req.pathVariable("slug"))
            .flatMap { editTalkViewDetail(req, it, emptyMap()) }

    private fun editTalkViewDetail(req: ServerRequest, talk: CachedTalk, errors: Map<String, String>) =
        ok().render(
            TEMPLATE_EDIT,
            mapOf(
                Pair("talk", talk.toDto(req.language())),
                Pair("speakers", talk.speakers.map { it.toDto(req.language()) }),
                Pair("baseUri", UriUtils.encode(properties.baseUri, StandardCharsets.UTF_8)),
                Pair("hasErrors", errors.isNotEmpty()),
                Pair("errors", errors),
                Pair("languages", enumMatcher(talk) { talk.language })
            )
        )

    fun saveProfileTalk(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            service.findOne(formData["id"]!!).flatMap { talk ->

                val errors = mutableMapOf<String, String>()

                // Null check
                if (formData["title"].isNullOrBlank()) {
                    errors["title"] = "talk.form.error.title.required"
                }
                if (formData["summary"].isNullOrBlank()) {
                    errors["summary"] = "talk.form.error.summary.required"
                }
                if (formData["language"].isNullOrBlank()) {
                    errors["language"] = "talk.form.error.language.required"
                }

                if (errors.isNotEmpty()) {
                    editTalkViewDetail(req, talk, errors)
                }

                val updatedTalk = talk.copy(
                    title = formData["title"]!!,
                    summary = markdownValidator.sanitize(formData["summary"]!!),
                    description = markdownValidator.sanitize(formData["description"]),
                    language = Language.valueOf(formData["language"]!!)
                )

                // We want to control data to not save invalid things in our database
                if (!maxLengthValidator.isValid(updatedTalk.title, 255)) {
                    errors["title"] = "talk.form.error.title.size"
                }
                if (!markdownValidator.isValid(updatedTalk.summary)) {
                    errors["summary"] = "talk.form.error.summary"
                }
                if (!markdownValidator.isValid(updatedTalk.description)) {
                    errors["description"] = "talk.form.error.description"
                }
                if (errors.isEmpty()) {
                    // If everything is Ok we save the user
                    service.save(updatedTalk.toTalk()).then(seeOther("${properties.baseUri}/me"))
                } else {
                    editTalkViewDetail(req, updatedTalk, errors)
                }
            }
        }

    private fun loadSponsors(event: CachedEvent) =
        event.filterBySponsorLevel(SponsorshipLevel.GOLD).let { sponsors ->
            mapOf(
                Pair("sponsors-gold", sponsors.map { it.toSponsorDto() }),
                Pair("sponsors-others", event.sponsors.filterNot { sponsors.contains(it) }.map { it.toSponsorDto() })
            )
        }

    fun redirectFromId(req: ServerRequest) = service.findOne(req.pathVariable("id")).flatMap {
        permanentRedirect("${properties.baseUri}/${it.event}/${it.slug}")
    }

    fun redirectFromSlug(req: ServerRequest) = service.findBySlug(req.pathVariable("slug")).flatMap {
        permanentRedirect("${properties.baseUri}/${it.event}/${it.slug}")
    }
}
