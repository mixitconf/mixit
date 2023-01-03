package mixit.talk.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitProperties
import mixit.event.model.EventImagesService
import mixit.event.model.EventService
import mixit.favorite.repository.FavoriteRepository
import mixit.routes.MustacheI18n
import mixit.routes.MustacheI18n.EVENT
import mixit.routes.MustacheI18n.IMAGES
import mixit.routes.MustacheI18n.SPONSORS
import mixit.routes.MustacheI18n.TALKS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheI18n.YEAR
import mixit.routes.MustacheTemplate
import mixit.routes.MustacheTemplate.FeedbackWall
import mixit.routes.MustacheTemplate.Media
import mixit.routes.MustacheTemplate.MediaImages
import mixit.routes.MustacheTemplate.Schedule
import mixit.routes.MustacheTemplate.TalkDetail
import mixit.routes.MustacheTemplate.TalkEdit
import mixit.routes.MustacheTemplate.TalkList
import mixit.talk.model.CachedTalk
import mixit.talk.model.Language
import mixit.talk.model.TalkService
import mixit.user.handler.dto.toDto
import mixit.user.model.UserService
import mixit.util.currentNonEncryptedUserEmail
import mixit.util.enumMatcher
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.formatTalkDate
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
import org.springframework.web.reactive.function.server.queryParamOrNull
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class TalkHandler(
    private val service: TalkService,
    private val eventService: EventService,
    private val eventImagesService: EventImagesService,
    private val userService: UserService,
    private val properties: MixitProperties,
    private val favoriteRepository: FavoriteRepository,
    private val maxLengthValidator: MaxLengthValidator,
    private val markdownValidator: MarkdownValidator
) {

    companion object {
        fun media(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(
                year,
                req,
                topic,
                template = Media
            )

        fun images(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(
                year,
                req,
                topic,
                template = MediaImages,
                album = req.pathVariable("album"),
                url = req.queryParamOrNull("url")
            )

        fun mediaWithFavorites(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(
                year,
                req,
                topic,
                template = Media,
                filterOnFavorite = true
            )

        fun talks(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(year, req, topic)

        fun feedbackWall(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(year, req, topic, template = FeedbackWall)

        fun talksWithFavorites(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(year, req, topic, filterOnFavorite = true)
    }

    data class TalkViewConfig(
        val year: Int,
        val req: ServerRequest,
        val topic: String? = null,
        val filterOnFavorite: Boolean = false,
        val template: MustacheTemplate = TalkList,
        val album: String? = null,
        val url: String? = null
    ) {
        fun isList() = template == Media
    }

    suspend fun scheduleView(req: ServerRequest) =
        ok().renderAndAwait(Schedule.template, mapOf(TITLE to Schedule.title))


    private data class TalkKey(val date: String, val id: String = date.replace(" ", "").lowercase())

    suspend fun findByEventView(config: TalkViewConfig): ServerResponse {
        val currentUserEmail = config.req.currentNonEncryptedUserEmail()
        val talks = loadTalkAndFavorites(config, currentUserEmail).let { talks ->
            if (config.template == Media) talks.filter { it.video != null } else talks
        }
        val event = eventService.findByYear(config.year)
        val title = if (config.topic == null) "${config.template.title}|${config.year}" else
            "${config.template.title}.${config.topic}|${config.year}"
        val images = findImages(config)
        val closestImages = findClosestImages(config)

        return ok()
            .render(
                config.template.template,
                mapOf(
                    EVENT to event.toEvent(),
                    SPONSORS to userService.loadSponsors(event),
                    TALKS to (if (config.isList()) talks else talks.groupBy {
                        TalkKey(
                            it.date ?: event.toEvent().start.atStartOfDay().formatTalkDate(config.req.language())
                        )
                    }),
                    TITLE to title,
                    YEAR to config.year,
                    IMAGES to images,
                    "schedulingFileUrl" to event.schedulingFileUrl,
                    "filtered" to config.filterOnFavorite,
                    "topic" to config.topic,
                    "filtered" to config.filterOnFavorite,
                    "videoUrl" to event.videoUrl?.url?.toVimeoPlayerUrl(),
                    "hasPhotosOrVideo" to (event.videoUrl != null || event.photoUrls.isNotEmpty()),
                    "singleImage" to (config.url != null),
                    "previousImage" to closestImages?.first,
                    "nextImage" to closestImages?.second,
                    "hasTalk" to talks.isNotEmpty()
                )
            )
            .awaitSingle()
    }

    /**
     * When we display only one image we want to know the previous and the next images
     */
    private suspend fun findClosestImages(config: TalkViewConfig) =
        if (config.template == MediaImages && config.url != null) {
            val pictures = eventImagesService
                .findOneOrNull(config.year.toString())
                ?.sections
                ?.first { it.sectionId == config.album }
                ?.pictures
                ?.map { it.name }

            pictures
                ?.indexOf(config.url)
                ?.let {
                    Pair(
                        if (it > 0) pictures[it - 1] else null,
                        if (it < pictures.size - 1) pictures[it + 1] else null,
                    )
                }
        } else null

    private suspend fun findImages(config: TalkViewConfig) =
        // We have a specific behavior on the MediaImages template
        if (config.template == MediaImages) {
            // If an url is specified in the query param we display only this image
            if (config.url != null) {
                eventImagesService.findOneOrNull(config.year.toString()).let { images ->
                    images?.copy(sections = images.sections
                        .filter { it.sectionId == config.album }
                        .map { it.copy(pictures = it.pictures.filter { pic -> pic.name == config.url }) })
                }
            } else {
                // We select all the album images
                eventImagesService.findOneOrNull(config.year.toString()).let { images ->
                    images?.copy(sections = images.sections.filter { it.sectionId == config.album })
                }
            }
        } else {
            // For other we return all albums but with only the first image
            eventImagesService.findOneOrNull(config.year.toString()).let { images ->
                images?.copy(sections = images.sections.map { it.copy(pictures = listOf(it.pictures.first())) })
            }
        }

    private suspend fun loadTalkAndFavorites(config: TalkViewConfig, currentUserEmail: String): List<TalkDto> =
        if (currentUserEmail.isEmpty()) {
            service.findByEvent(config.year.toString(), config.topic).map { it.toDto(config.req.language()) }
        } else {
            val favoriteTalkIds = favoriteRepository.findByEmail(currentUserEmail).map { it.talkId }
            if (config.filterOnFavorite) {
                service.findByEventAndTalkIds(config.year.toString(), favoriteTalkIds, config.topic).map {
                    it.toDto(config.req.language(), favorite = true)
                }
            } else {
                service.findByEvent(config.year.toString(), config.topic).map {
                    it.toDto(config.req.language(), favorite = favoriteTalkIds.contains(it.id))
                }
            }
        }

    suspend fun findOneView(req: ServerRequest, year: Int): ServerResponse {
        val lang = req.language()
        val currentUserEmail = req.currentNonEncryptedUserEmail()
        val event = eventService.findByYear(year)
        val favoriteTalkIds = favoriteRepository.findByEmail(currentUserEmail).map { it.talkId }
        val talk = service.findByEventAndSlug(year.toString(), req.pathVariable("slug"))
        val speakers = talk.speakers.map { it.toDto(lang) }.sortedBy { it.firstname }
        val otherTalks = service.findBySpeakerId(speakers.map { it.login }, talk.id).map { it.toDto(lang) }

        return ok()
            .render(
                TalkDetail.template,
                mapOf(
                    YEAR to year,
                    TITLE to "talk.html.title|${talk.title}",
                    SPONSORS to userService.loadSponsors(event),
                    "talk" to talk.toDto(req.language()),
                    "speakers" to talk.speakers.map { it.toDto(lang) }.sortedBy { it.firstname },
                    "othertalks" to otherTalks,
                    "favorites" to favoriteTalkIds.any { talk.id == it },
                    "hasOthertalks" to otherTalks.isNotEmpty(),
                    "vimeoPlayer" to talk.video.toVimeoPlayerUrl()
                )
            )
            .awaitSingle()
    }

    suspend fun editTalkView(req: ServerRequest): ServerResponse {
        val talk = service.findBySlug(req.pathVariable("slug"))
        return editTalkViewDetail(req, talk, emptyMap())
    }

    private suspend fun editTalkViewDetail(
        req: ServerRequest,
        talk: CachedTalk,
        errors: Map<String, String>
    ): ServerResponse {
        val params = mapOf(
            MustacheI18n.TALK to talk.toDto(req.language()),
            MustacheI18n.SPEAKERS to talk.speakers.map { it.toDto(req.language()) },
            MustacheI18n.HAS_ERRORS to errors.isNotEmpty(),
            MustacheI18n.ERRORS to errors,
            MustacheI18n.LANGUAGES to enumMatcher(talk) { talk.language }
        )
        return ok().render(TalkEdit.template, params).awaitSingle()
    }

    suspend fun saveProfileTalk(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val talk = service.findOneOrNull(formData["id"]!!) ?: throw NotFoundException()

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
            service.save(updatedTalk.toTalk()).awaitSingle()
            return seeOther("${properties.baseUri}/me")
        } else {
            return editTalkViewDetail(req, updatedTalk, errors)
        }
    }

    suspend fun redirectFromId(req: ServerRequest): ServerResponse {
        val talk = service.findOneOrNull("id") ?: throw NotFoundException()
        return permanentRedirect("${properties.baseUri}/${talk.event}/${talk.slug}")
    }

    suspend fun redirectFromSlug(req: ServerRequest): ServerResponse {
        val talk = service.findBySlug(req.pathVariable("slug"))
        return permanentRedirect("${properties.baseUri}/${talk.event}/${talk.slug}")
    }
}
