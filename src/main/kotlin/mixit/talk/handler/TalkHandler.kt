package mixit.talk.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitApplication.Companion.TIMEZONE
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
import mixit.talk.handler.TalkHandler.TalkListType.Agenda
import mixit.talk.handler.TalkHandler.TalkListType.ListByDate
import mixit.talk.handler.TalkHandler.TalkListType.SimpleList
import mixit.talk.model.CachedTalk
import mixit.talk.model.Language
import mixit.talk.model.Room
import mixit.talk.model.TalkFormat
import mixit.talk.model.TalkService
import mixit.user.handler.dto.toDto
import mixit.user.model.UserService
import mixit.util.currentNonEncryptedUserEmail
import mixit.util.enumMatcher
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.formatTalkDate
import mixit.util.formatTalkTime
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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

        fun mediaWithFavorites(req: ServerRequest, year: Int, topic: String? = null) =
            media(req, year, topic).copy(filterOnFavorite = true)

        fun images(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(
                year,
                req,
                topic,
                template = MediaImages,
                album = req.pathVariable("album"),
                url = req.queryParamOrNull("url")
            )

        fun talks(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(
                year,
                req,
                topic,
                viewList = if ((req.queryParamOrNull("agenda") ?: "true") == "true") Agenda else ListByDate,
                viewWorkshop = (req.queryParamOrNull("workshop") ?: "true") == "true"
            )

        fun talksWithFavorites(req: ServerRequest, year: Int, topic: String? = null) =
            talks(req, year, topic).copy(filterOnFavorite = true)

        fun feedbackWall(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(year, req, topic, template = FeedbackWall)
    }

    enum class TalkListType { SimpleList, ListByDate, Agenda }
    data class TalkViewConfig(
        val year: Int,
        val req: ServerRequest,
        val topic: String? = null,
        val filterOnFavorite: Boolean = false,
        val template: MustacheTemplate = TalkList,
        val album: String? = null,
        val url: String? = null,
        val viewList: TalkListType = SimpleList,
        val viewWorkshop: Boolean = true
    )

    suspend fun scheduleView(req: ServerRequest) =
        ok().renderAndAwait(Schedule.template, mapOf(TITLE to Schedule.title))


    private data class TalkKey(val date: String, val id: String = date.replace(" ", "").lowercase())

    suspend fun findByEventView(config: TalkViewConfig): ServerResponse {
        val currentUserEmail = config.req.currentNonEncryptedUserEmail()
        val talks = filterTalkByFormat(
            loadTalkAndFavorites(config, currentUserEmail).let { talks ->
                if (config.template == Media) talks.filter { it.video != null } else talks
            }, config.viewWorkshop
        )
        val event = eventService.findByYear(config.year)
        val title = if (config.topic == null) "${config.template.title}|${config.year}" else
            "${config.template.title}.${config.topic}|${config.year}"
        val images = findImages(config)
        val closestImages = findClosestImages(config)
        val days = (0..Duration.between(event.start.atStartOfDay(), event.end.atStartOfDay())
            .toDays()).map { event.start.plusDays(it) }
        val rooms = roomsToDisplayOnAgenda(talks)
        val canDisplayAgenda = rooms.isNotEmpty() && !(rooms.contains(Room.UNKNOWN) && rooms.size==1)

        return ok()
            .render(
                config.template.template,
                mapOf(
                    EVENT to event.toEvent(),
                    SPONSORS to userService.loadSponsors(event),
                    TALKS to when (config.viewList) {
                        SimpleList -> talks
                        Agenda -> {
                            if(canDisplayAgenda) {
                                talksToDisplayOnAgenda(talks, rooms, days, config.req.language())
                            }
                            else {
                                talksToDisplayByDate(talks, days, config.req.language())
                            }
                        }
                        ListByDate -> {
                            talksToDisplayByDate(talks, days, config.req.language())
                        }
                    },
                    TITLE to title,
                    YEAR to config.year,
                    IMAGES to images,
                    "canDisplayAgenda" to canDisplayAgenda,
                    "displayAgenda" to (config.viewList == Agenda && canDisplayAgenda),
                    "displayWorkshop" to config.viewWorkshop,
                    "schedulingFileUrl" to event.schedulingFileUrl,
                    "filtered" to config.filterOnFavorite,
                    "topic" to config.topic,
                    "filtered" to config.filterOnFavorite,
                    "videoUrl" to event.videoUrl?.url?.toVimeoPlayerUrl(),
                    "hasPhotosOrVideo" to (event.videoUrl != null || event.photoUrls.isNotEmpty()),
                    "singleImage" to (config.url != null),
                    "previousImage" to closestImages?.first,
                    "nextImage" to closestImages?.second,
                    "hasTalk" to talks.isNotEmpty(),
                    "isCurrent" to (config.year == CURRENT_EVENT.toInt())
                )
            )
            .awaitSingle()
    }

    private fun roomsToDisplayOnAgenda(filteredTalks: List<TalkDto>): List<Room> =
        filteredTalks
            .asSequence()
            .filterNot { it.title.lowercase().contains("mixteen") }
            .map { it.room ?: "rooms.${Room.UNKNOWN.name.uppercase()}" }
            .distinct()
            .map { Room.valueOf(it.replace("rooms.", "").uppercase()) }
            .filter { !listOf(Room.MUMMY, Room.OUTSIDE).contains(it) }
            .sortedBy { it.name }
            .toList()

    private fun talksToDisplayByDate(
        filteredTalks: List<TalkDto>,
        days: List<LocalDate>,
        language: Language
    ): Map<String, DayTalksDto> {
        if (filteredTalks.isEmpty()) {
            return emptyMap()
        }
        val keys: Map<String, LocalDate> = (1..days.size).associate { "day${it}" to days[it - 1] }
        return keys.entries.associate { (day, localDate) ->
            day to DayTalksDto(
                day,
                localDate.atStartOfDay().formatTalkDate(language),
                day == "day1",
                filteredTalks.filter { it.startLocalDateTime ==null ||  it.startLocalDateTime?.toLocalDate() == localDate }
            )
        }
    }

    /**
     * This function is used to build a grid to display talk in a screen that looks like an agenda. We
     * can choose a Workshop view or a talk view
     */
    private fun talksToDisplayOnAgenda(
        filteredTalks: List<TalkDto>,
        rooms: List<Room>,
        days: List<LocalDate>,
        language: Language
    ): Map<String, DayRoomTalksDto> {
        if (rooms.isEmpty() || filteredTalks.isEmpty()) {
            return emptyMap()
        }
        val keys: Map<String, LocalDate> = (1..days.size).associate { "day${it}" to days[it - 1] }
        return keys.entries.associate { (day, localDate) ->
            day to DayRoomTalksDto(
                day,
                localDate.atStartOfDay().formatTalkDate(language),
                day == "day1",
                computeDaySlices(
                    localDate,
                    filteredTalks.filter { it.startLocalDateTime?.toLocalDate() == localDate },
                    rooms,
                    language
                )
            )
        }
    }

    private fun computeDaySlices(
        day: LocalDate,
        filteredTalks: List<TalkDto>,
        rooms: List<Room>,
        language: Language
    ): List<RoomDaySliceDto> {
        val roomName = listOf(null) + rooms.map { "rooms.${it.name.lowercase()}" }
        return roomName.map { room ->
            RoomDaySliceDto(room, computeTalkByRooms(day, room, filteredTalks, language))
        }
    }

    private fun computeTalkByRooms(
        day: LocalDate,
        room: String?,
        filteredTalks: List<TalkDto>,
        language: Language
    ): List<RoomTalkDto> {
        // The start is 8:00 and the last 19:00
        val start = 9L
        val end = 19L
        val startOfConference = day.atStartOfDay(ZoneId.of(TIMEZONE)).plusHours(start).toLocalDateTime()
        val sliceNumber = 12 * (end - start)

        val roomTalkDtos = (0..sliceNumber)
            .map { sliceIndex ->
                val sliceStart = startOfConference.plusMinutes(sliceIndex * 5L)
                val talkOnSliceAndRoom = room?.let { roomName ->
                    filteredTalks.firstOrNull { it.startLocalDateTime == sliceStart && it.room == roomName }
                }
                val bordered = sliceStart.minute % 10 == 0
                val displayed = if (room == null) {
                    filteredTalks.any { it.startLocalDateTime == sliceStart } || bordered
                } else {
                    talkOnSliceAndRoom != null
                }
                val sliceDuration = talkOnSliceAndRoom?.let {
                    Duration.between(it.startLocalDateTime, it.endLocalDateTime).toMinutes() / 5
                } ?: 1


                RoomTalkDto(
                    sliceStart.formatTalkTime(language),
                    sliceStart,
                    displayed,
                    bordered,
                    sliceDuration,
                    talkOnSliceAndRoom
                )
            }

        return mutableListOf<RoomTalkDto>().also {
            for (index in roomTalkDtos.indices) {
                val roomTalkDto = roomTalkDtos[index]
                val sliceDuration = inspectPrevious(it, index, roomTalkDto.sliceDuration)
                it.add(roomTalkDto.copy(sliceDuration = sliceDuration))
            }
        }.filter { it.sliceDuration > 0 && !(it.start.hour > end - 1 && it.start.minute < 40) }
    }

    private fun inspectPrevious(
        roomTalkDtos: List<RoomTalkDto>,
        index: Int,
        sliceDuration: Long,
        iteration: Long = 1
    ): Long {
        if (index > 0) {
            val previousSliceDuration = roomTalkDtos[index - 1].sliceDuration
            if (previousSliceDuration == 0L) {
                return inspectPrevious(roomTalkDtos, index - 1, sliceDuration, iteration + 1)
            }
            if (previousSliceDuration > 1) {
                if (previousSliceDuration > iteration) {
                    return 0L
                }
            }
        }
        return sliceDuration
    }

    private suspend fun filterTalkByFormat(talks: List<TalkDto>, seeWorkshop: Boolean) =
        if (seeWorkshop) talks else talks.filterNot { it.format == TalkFormat.WORKSHOP }

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
                images?.copy(
                    sections = images.sections.map { section ->
                        section.copy(pictures = section.pictures.firstOrNull()?.let { listOf(it) } ?: emptyList())
                    })
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

    private data class DayTalksDto(
        val id: String,
        val day: String,
        val active: Boolean,
        val talks: List<TalkDto>
    )
    private data class DayRoomTalksDto(
        val id: String,
        val day: String,
        val active: Boolean,
        val slices: List<RoomDaySliceDto>
    )

    private data class RoomDaySliceDto(val room: String?, val talkByRooms: List<RoomTalkDto>)

    private data class RoomTalkDto(
        val time: String,
        val start: LocalDateTime,
        val timeDisplayed: Boolean,
        val bordered: Boolean,
        val sliceDuration: Long = 1,
        val talk: TalkDto?
    )
}
