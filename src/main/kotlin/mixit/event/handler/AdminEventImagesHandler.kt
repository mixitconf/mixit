package mixit.event.handler

import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitProperties
import mixit.event.model.EventImage
import mixit.event.model.EventImages
import mixit.event.model.EventImagesSection
import mixit.event.model.EventImagesService
import mixit.routes.MustacheI18n
import mixit.routes.MustacheI18n.CREATION_MODE
import mixit.routes.MustacheI18n.EVENT
import mixit.routes.MustacheI18n.EVENTS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate
import mixit.routes.MustacheTemplate.AdminEvent
import mixit.routes.MustacheTemplate.AdminEventImage
import mixit.routes.MustacheTemplate.AdminEventImages
import mixit.talk.model.TalkService
import mixit.util.enumMatcher
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.queryParamOrNull
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class AdminEventImagesHandler(
    private val service: EventImagesService,
    private val talkService: TalkService,
    private val properties: MixitProperties
) {

    companion object {
        const val LIST_URI = "/admin/events/images"
    }

    suspend fun adminEventImages(req: ServerRequest): ServerResponse {
        val events = service.findAll().sortedBy { it.id }
        return ok().renderAndAwait(
            AdminEventImages.template,
            mapOf(
                TITLE to AdminEventImages.title,
                EVENTS to events
            )
        )
    }

    private suspend fun adminEditEventImages(
        eventImages: EventImages = EventImages(
            null,
            emptyList()
        )
    ): ServerResponse =
        ok().renderAndAwait(
            AdminEventImage.template,
            mapOf(
                TITLE to AdminEvent.title,
                CREATION_MODE to (eventImages.event == null),
                EVENT to eventImages
            )
        )

    suspend fun createEventImages(req: ServerRequest): ServerResponse =
        this.adminEditEventImages()

    suspend fun editEventImages(req: ServerRequest): ServerResponse =
        service
            .findOneOrNull(req.pathVariable("id"))
            ?.let { this.adminEditEventImages(it.toEventImages()) }
            ?: throw NotFoundException()

    suspend fun adminSaveEventImages(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        // We need to find the event in database
        val existingEvent = service.findOneOrNull(formData["event"] ?: throw NotFoundException())?.toEventImages()
        val updatedEvent = existingEvent ?: EventImages(formData["event"]!!, emptyList())
        service.save(updatedEvent).awaitSingleOrNull()
        return seeOther("${properties.baseUri}${AdminEventHandler.LIST_URI}")
    }

    suspend fun editEventImagesSection(req: ServerRequest): ServerResponse {
        val event = service.findOneOrNull(req.pathVariable("event"))?.toEventImages() ?: throw NotFoundException()
        return adminEventImagesSection(
            event.event!!.toString(),
            event.sections.first { it.sectionId == req.pathVariable("sectionId") }
        )
    }

    suspend fun createEventImagesSection(req: ServerRequest): ServerResponse =
        adminEventImagesSection(req.pathVariable("event"))

    private suspend fun adminEventImagesSection(eventId: String, section: EventImagesSection? = null): ServerResponse =
        ok().renderAndAwait(
            MustacheTemplate.AdminEventImagesSection.template,
            mapOf(
                CREATION_MODE to (section == null),
                MustacheI18n.EVENT_ID to eventId,
                "section" to section
            )
        )

    suspend fun adminUpdateEventImagesSection(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEventImages(formData)
        val sections = event.sections.map {
            if (it.sectionId == formData["sectionId"]!!) {
                it.copy(i18n = formData["i18n"]!!)
            } else it
        }
        service.save(event.copy(sections = sections)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.event}")
    }

    suspend fun adminCreateEventImagesSection(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEventImages(formData)
        val sections = event.sections.toMutableList()
        sections.add(EventImagesSection(formData["sectionId"]!!, formData["i18n"]!!, emptyList()))
        service.save(event.copy(sections = sections)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.event}")
    }

    suspend fun adminDeleteEventImagesSection(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEventImages(formData)
        val sections = event.sections.filter { it.sectionId != formData["sectionId"]!! }
        service.save(event.copy(sections = sections)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.event}")
    }

    private suspend fun loadEventImages(formData: Map<String, String?>): EventImages =
        service
            .findOneOrNull(formData["eventId"] ?: throw NotFoundException())
            ?.toEventImages()
            ?: throw NotFoundException()

    suspend fun editEventImagesSectionImage(req: ServerRequest): ServerResponse {
        val event = service.findOneOrNull(req.pathVariable("event"))?.toEventImages() ?: throw NotFoundException()
        val section =
            event.sections.firstOrNull { it.sectionId == req.pathVariable("sectionId") } ?: throw NotFoundException()
        return adminEventImagesSectionImage(
            req.pathVariable("event"),
            req.pathVariable("sectionId"),
            section.images.firstOrNull { it.name == req.queryParamOrNull("url") }
        )
    }

    suspend fun createEventImagesSectionImage(req: ServerRequest): ServerResponse =
        adminEventImagesSectionImage(req.pathVariable("event"), req.pathVariable("sectionId"))

    private suspend fun adminEventImagesSectionImage(
        eventId: String,
        sectionId: String,
        image: EventImage? = null
    ): ServerResponse {
        val talks = talkService.findByEvent(eventId).sortedBy { it.title }.map {
            Triple(it.id, "${it.title} (${it.format})", it.id.contains(image?.talkId ?: "oops"))
        }
        return ok().renderAndAwait(
            MustacheTemplate.AdminEventImagesSectionImage.template,
            mapOf(
                CREATION_MODE to (image == null),
                MustacheI18n.EVENT_ID to eventId,
                "sectionId" to sectionId,
                "image" to image,
                "templates" to enumMatcher(image) { it?.mustacheTemplate ?: MustacheTemplate.None },
                MustacheI18n.TALKS to talks,
                "notalk" to talks.isEmpty()
            )
        )
    }

    suspend fun adminUpdateEventImagesSectionImage(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEventImages(formData)
        val section = loadEventSection(formData)
        val images = section.images.map { img ->
            if (img.name == formData["name"]!!) {
                img.copy(
                    talkId = formData["talkId"],
                    mustacheTemplate = formData["template"]?.let { MustacheTemplate.valueOf(it) }
                )
            } else img
        }
        service.save(
            event.copy(
                sections = event.sections.map {
                    if (it.sectionId == section.sectionId) section.copy(
                        images = images
                    ) else it
                }
            )
        ).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/${event.event}/sections/edit/${section.sectionId}")
    }

    suspend fun adminCreateEventImagesSectionImage(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEventImages(formData)
        val section = loadEventSection(formData)
        val images = section.images.toMutableList()
        images.add(
            EventImage(
                formData["name"]!!,
                formData["talkId"],
                formData["template"]?.let { MustacheTemplate.valueOf(it) }
            )
        )
        service.save(
            event.copy(
                sections = event.sections.map {
                    if (it.sectionId == section.sectionId) section.copy(
                        images = images
                    ) else it
                }
            )
        ).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/${event.event}/sections/edit/${section.sectionId}")
    }

    suspend fun adminDeleteEventImagesSectionImage(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEventImages(formData)
        val section = loadEventSection(formData)
        val images = section.images.filter { it.name != formData["name"]!! }
        service.save(
            event.copy(
                sections = event.sections.map {
                    if (it.sectionId == section.sectionId) section.copy(
                        images = images
                    ) else it
                }
            )
        ).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/${event.event}/sections/edit/${section.sectionId}")
    }

    private suspend fun loadEventSection(formData: Map<String, String?>): EventImagesSection =
        loadEventImages(formData).sections
            .firstOrNull { it.sectionId == formData["sectionId"] }
            ?: throw NotFoundException()
}
