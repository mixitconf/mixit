package mixit.event.handler

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitProperties
import mixit.event.model.Event
import mixit.event.model.EventOrganization
import mixit.event.model.EventOrganizer
import mixit.event.model.EventService
import mixit.event.model.EventSponsoring
import mixit.event.model.EventVolunteer
import mixit.event.model.SponsorshipLevel
import mixit.util.AdminUtils.toJson
import mixit.util.AdminUtils.toLink
import mixit.util.AdminUtils.toLinks
import mixit.util.enumMatcher
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.mustache.MustacheI18n.CREATION_MODE
import mixit.util.mustache.MustacheI18n.EVENT
import mixit.util.mustache.MustacheI18n.EVENTS
import mixit.util.mustache.MustacheI18n.EVENT_ID
import mixit.util.mustache.MustacheI18n.LINKS
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheTemplate.AdminEvent
import mixit.util.mustache.MustacheTemplate.AdminEventOrganization
import mixit.util.mustache.MustacheTemplate.AdminEventOrganizer
import mixit.util.mustache.MustacheTemplate.AdminEventSponsor
import mixit.util.mustache.MustacheTemplate.AdminEventVolunteer
import mixit.util.mustache.MustacheTemplate.AdminEvents
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class AdminEventHandler(
    private val service: EventService,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper
) {

    companion object {
        const val LIST_URI = "/admin/events"
    }

    suspend fun adminEvents(req: ServerRequest): ServerResponse {
        val events = service.findAll().sortedBy { it.id }.map { it.toEvent() }
        return ok().renderAndAwait(
            AdminEvents.template,
            mapOf(
                TITLE to AdminEvents.title,
                EVENTS to events
            )
        )
    }

    suspend fun createEvent(req: ServerRequest): ServerResponse =
        this.adminEvent()

    suspend fun editEvent(req: ServerRequest): ServerResponse =
        service
            .findOneOrNull(req.pathVariable("eventId"))
            ?.let { this.adminEvent(it.toEvent()) }
            ?: throw NotFoundException()

    private suspend fun adminEvent(event: Event = Event()): ServerResponse =
        ok().renderAndAwait(
            AdminEvent.template,
            mapOf(
                TITLE to AdminEvent.title,
                CREATION_MODE to event.id.isEmpty(),
                EVENT to event,
                LINKS to event.photoUrls.toJson(objectMapper),
                "videolink" to (event.videoUrl?.toJson(objectMapper) ?: "")
            )
        )

    suspend fun adminSaveEvent(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        // We need to find the event in database
        val existingEvent = service.findOneOrNull(formData["eventId"] ?: throw NotFoundException())?.toEvent()
        val updatedEvent = if (existingEvent == null) {
            Event(
                formData["eventId"]!!,
                LocalDate.parse(formData["start"]!!),
                LocalDate.parse(formData["end"]!!),
                formData["current"]?.toBoolean() ?: false,
                photoUrls = formData["photoUrls"]?.toLinks(objectMapper) ?: emptyList(),
                videoUrl = formData["videoUrl"]?.toLink(objectMapper),
                schedulingFileUrl = formData["schedulingFileUrl"]
            )
        } else {
            Event(
                existingEvent.id,
                LocalDate.parse(formData["start"]!!),
                LocalDate.parse(formData["end"]!!),
                formData["current"]?.toBoolean() ?: false,
                existingEvent.sponsors,
                existingEvent.organizations,
                existingEvent.volunteers,
                existingEvent.organizers,
                formData["photoUrls"]?.toLinks(objectMapper) ?: emptyList(),
                formData["videoUrl"]?.toLink(objectMapper),
                formData["schedulingFileUrl"]
            )
        }
        service.save(updatedEvent).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    suspend fun createEventSponsoring(req: ServerRequest): ServerResponse =
        adminEventSponsoring(req.pathVariable("eventId"))

    private suspend fun adminEventSponsoring(eventId: String, eventSponsoring: EventSponsoring = EventSponsoring()) =
        ok().renderAndAwait(
            AdminEventSponsor.template,
            mapOf(
                TITLE to AdminEventSponsor.title,
                CREATION_MODE to (eventSponsoring.sponsorId == ""),
                EVENT_ID to eventId,
                "eventSponsoring" to eventSponsoring,
                "levels" to enumMatcher(eventSponsoring) { it?.level ?: SponsorshipLevel.NONE }
            )
        )

    suspend fun adminUpdateEventSponsoring(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        // We need to find the event in database
        val event = service.findOneOrNull(formData["eventId"]!!)
            ?.toEvent()
            ?: throw NotFoundException()

        val updatedSponsors = event.sponsors.map { sponsoring ->
            if (sponsoring.sponsorId == formData["sponsorId"] && sponsoring.level.name == formData["level"]) {
                EventSponsoring(
                    sponsoring.level,
                    sponsoring.sponsorId,
                    formData["subscriptionDate"]?.let { LocalDate.parse(it) } ?: LocalDate.now()
                )
            } else {
                sponsoring
            }
        }
        service.save(event.copy(sponsors = updatedSponsors)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}")
    }

    suspend fun adminCreateEventSponsoring(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        // We need to find the event in database
        val event = service.findOneOrNull(formData["eventId"] ?: throw NotFoundException())
            ?.toEvent()
            ?: throw NotFoundException()

        // We create a mutable list to add the new element
        val sponsors = event.sponsors.toMutableList().apply {
            add(
                EventSponsoring(
                    SponsorshipLevel.valueOf(formData["level"]!!),
                    formData["sponsorId"]!!,
                    formData["subscriptionDate"]?.let { LocalDate.parse(it) } ?: LocalDate.now()
                )
            )
        }

        service.save(event.copy(sponsors = sponsors)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}")
    }

    suspend fun adminDeleteEventSponsoring(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        // We need to find the event in database
        val event = loadEvent(formData)

        val newSponsors = event.sponsors
            .filterNot { it.sponsorId == formData["sponsorId"] && it.level.name == formData["level"] }

        // We create a mutable list
        service.save(event.copy(sponsors = newSponsors)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}")
    }

    suspend fun editEventSponsoring(req: ServerRequest): ServerResponse {
        val event = service.findOneOrNull(req.pathVariable("eventId"))?.toEvent() ?: throw NotFoundException()
        return adminEventSponsoring(
            event.id,
            event.sponsors.first {
                it.sponsorId == req.pathVariable("sponsorId") && it.level.name == req.pathVariable("level")
            }
        )
    }

    suspend fun editEventOrga(req: ServerRequest): ServerResponse {
        val event = service.findOneOrNull(req.pathVariable("eventId"))?.toEvent() ?: throw NotFoundException()
        return adminEventOrganization(
            event.id,
            event.organizations.first { it.organizationLogin == req.pathVariable("organizationLogin") }
        )
    }

    suspend fun createEventOrganization(req: ServerRequest): ServerResponse =
        adminEventOrganization(req.pathVariable("eventId"))

    private suspend fun adminEventOrganization(eventId: String, eventOrganization: EventOrganization? = null) =
        ok().renderAndAwait(
            AdminEventOrganization.template,
            mapOf(
                TITLE to AdminEventOrganization.title,
                CREATION_MODE to (eventOrganization == null),
                EVENT_ID to eventId,
                "eventOrganization" to eventOrganization
            )
        )

    suspend fun adminUpdateEventOrganization(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        // For the moment we have noting to save
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminCreateEventOrganization(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        val organizations = event.organizations.toMutableList().apply {
            add(EventOrganization(formData["organizationLogin"]!!))
        }
        service.save(event.copy(organizations = organizations)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminDeleteEventOrganization(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        val organizations = event.organizations.filter { it.organizationLogin != formData["organizationLogin"]!! }
        service.save(event.copy(organizations = organizations)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun editEventVolunteer(req: ServerRequest): ServerResponse {
        val event = service.findOneOrNull(req.pathVariable("eventId"))?.toEvent() ?: throw NotFoundException()
        return adminEventVolunteer(
            event.id,
            event.volunteers.first { it.volunteerLogin.contains(req.pathVariable("volunteerLogin")) }
        )
    }

    suspend fun editEventOrganizer(req: ServerRequest): ServerResponse {
        val event = service.findOneOrNull(req.pathVariable("eventId"))?.toEvent() ?: throw NotFoundException()
        return adminEventOrganizer(
            event.id,
            event.organizers.first { it.organizerLogin == req.pathVariable("organizerLogin") }
        )
    }

    suspend fun createEventVolunteer(req: ServerRequest): ServerResponse =
        adminEventVolunteer(req.pathVariable("eventId"))

    suspend fun createEventOrganizer(req: ServerRequest): ServerResponse =
        adminEventOrganizer(req.pathVariable("eventId"))

    private suspend fun adminEventVolunteer(eventId: String, eventVolunteer: EventVolunteer? = null): ServerResponse =
        ok().renderAndAwait(
            AdminEventVolunteer.template,
            mapOf(
                CREATION_MODE to (eventVolunteer == null),
                EVENT_ID to eventId,
                "eventVolunteer" to eventVolunteer
            )
        )

    private suspend fun adminEventOrganizer(eventId: String, eventOrganizer: EventOrganizer? = null): ServerResponse =
        ok().renderAndAwait(
            AdminEventOrganizer.template,
            mapOf(
                CREATION_MODE to (eventOrganizer == null),
                EVENT_ID to eventId,
                "eventOrganizer" to eventOrganizer
            )
        )

    suspend fun adminUpdateEventVolunteer(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        // For the moment we have noting to save
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminUpdateEventOrganizer(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        // For the moment we have noting to save
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminCreateEventVolunteer(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        val volunteers = event.volunteers.toMutableList()
        volunteers.add(EventVolunteer(formData["volunteerLogin"]!!))
        service.save(event.copy(volunteers = volunteers)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminCreateEventOrganizer(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        val organizers = event.organizers.toMutableList()
        organizers.add(EventOrganizer(formData["organizerLogin"]!!))
        service.save(event.copy(organizers = organizers)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminDeleteEventVolunteer(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        val volunteers =
            event.volunteers.filter { it.volunteerLogin != formData["volunteerLogin"]!! }
        service.save(event.copy(volunteers = volunteers)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminDeleteEventOrganizer(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val event = loadEvent(formData)
        val organizers =
            event.organizers.filter { it.organizerLogin != formData["organizerLogin"]!! }
        service.save(event.copy(organizers = organizers)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    private suspend fun loadEvent(formData: Map<String, String?>): Event =
        service
            .findOneOrNull(formData["eventId"] ?: throw NotFoundException())
            ?.toEvent()
            ?: throw NotFoundException()
}
