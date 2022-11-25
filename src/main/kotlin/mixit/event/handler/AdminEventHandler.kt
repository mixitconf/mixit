package mixit.event.handler

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitProperties
import mixit.event.model.Event
import mixit.event.model.EventOrganization
import mixit.event.model.EventService
import mixit.event.model.EventSponsoring
import mixit.event.model.EventVolunteer
import mixit.event.model.SponsorshipLevel
import mixit.routes.MustacheI18n.CREATION_MODE
import mixit.routes.MustacheI18n.EVENT
import mixit.routes.MustacheI18n.EVENTS
import mixit.routes.MustacheI18n.EVENT_ID
import mixit.routes.MustacheI18n.LINKS
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate
import mixit.routes.MustacheTemplate.AdminEventOrganization
import mixit.routes.MustacheTemplate.AdminEventSponsor
import mixit.routes.MustacheTemplate.AdminEventVolunteer
import mixit.routes.MustacheTemplate.AdminEvents
import mixit.util.AdminUtils.toJson
import mixit.util.AdminUtils.toLink
import mixit.util.AdminUtils.toLinks
import mixit.util.coExtractFormData
import mixit.util.enumMatcher
import mixit.util.errors.NotFoundException
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
import java.time.LocalDate

@Component
class AdminEventHandler(
    private val service: EventService,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper
) {

    companion object {
        const val LIST_URI = "/admin/events"
        const val CURRENT_EVENT = "2022"
        const val TIMEZONE = "Europe/Paris"
    }

    suspend fun adminEvents(req: ServerRequest): ServerResponse {
        val events = service.coFindAll().sortedBy { it.id }.map { it.toEvent() }
        return ok().renderAndAwait(
            AdminEvents.template,
            mapOf(
                TITLE to "admin.events.title",
                EVENTS to events
            )
        )
    }

    suspend fun createEvent(req: ServerRequest): ServerResponse =
        this.adminEvent()

    suspend fun editEvent(req: ServerRequest): ServerResponse =
        service
            .coFindOne(req.pathVariable("eventId"))
            .let { this.adminEvent(it.toEvent()) }

    private suspend fun adminEvent(event: Event = Event()): ServerResponse =
        ok().renderAndAwait(
            MustacheTemplate.AdminEvent.template,
            mapOf(
                CREATION_MODE to event.id.isEmpty(),
                EVENT to event,
                LINKS to event.photoUrls.toJson(objectMapper),
                "videolink" to (event.videoUrl?.toJson(objectMapper) ?: "")
            )
        )

    suspend fun adminSaveEvent(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        // We need to find the event in database
        val existingEvent = service.coFindOneOrNull(formData["eventId"] ?: throw NotFoundException())?.toEvent()
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
                CREATION_MODE to (eventSponsoring.sponsorId == ""),
                EVENT_ID to eventId,
                "eventSponsoring" to eventSponsoring,
                "levels" to enumMatcher(eventSponsoring) { it?.level ?: SponsorshipLevel.NONE }
            )
        )

    suspend fun adminUpdateEventSponsoring(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        // We need to find the event in database
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException()).toEvent()

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
        val formData = req.coExtractFormData()
        // We need to find the event in database
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException()).toEvent()

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
        val formData = req.coExtractFormData()
        // We need to find the event in database
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException()).toEvent()

        val newSponsors = event.sponsors
            .filterNot { it.sponsorId == formData["sponsorId"] && it.level.name == formData["level"] }

        // We create a mutable list
        service.save(event.copy(sponsors = newSponsors)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}")
    }

    suspend fun editEventSponsoring(req: ServerRequest): ServerResponse {
        val event = service.coFindOne(req.pathVariable("eventId")).toEvent()
        return adminEventSponsoring(
            event.id,
            event.sponsors.first {
                it.sponsorId == req.pathVariable("sponsorId") && it.level.name == req.pathVariable("level")
            }
        )
    }

    suspend fun editEventOrga(req: ServerRequest): ServerResponse {
        val event = service.coFindOne(req.pathVariable("eventId")).toEvent()
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
                CREATION_MODE to (eventOrganization == null),
                EVENT_ID to eventId,
                "eventOrganization" to eventOrganization
            )
        )

    suspend fun adminUpdateEventOrganization(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException())
        // For the moment we have noting to save
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminCreateEventOrganization(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException()).toEvent()
        val organizations = event.organizations.toMutableList().apply {
            add(EventOrganization(formData["organizationLogin"]!!))
        }
        service.save(event.copy(organizations = organizations)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminDeleteEventOrganization(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException()).toEvent()
        val organizations = event.organizations.filter { it.organizationLogin != formData["organizationLogin"]!! }
        service.save(event.copy(organizations = organizations)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun editEventVolunteer(req: ServerRequest): ServerResponse {
        val event = service.coFindOne(req.pathVariable("eventId")).toEvent()
        return adminEventVolunteer(
            event.id,
            event.volunteers.first { it.volunteerLogin == req.pathVariable("volunteerLogin") }
        )
    }

    suspend fun createEventVolunteer(req: ServerRequest): ServerResponse =
        adminEventVolunteer(req.pathVariable("eventId"))

    private suspend fun adminEventVolunteer(eventId: String, eventVolunteer: EventVolunteer? = null): ServerResponse =
        ok().renderAndAwait(
            AdminEventVolunteer.template,
            mapOf(
                CREATION_MODE to (eventVolunteer == null),
                EVENT_ID to eventId,
                "eventOrganization" to eventVolunteer
            )
        )

    suspend fun adminUpdateEventVolunteer(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException()).toEvent()
        // For the moment we have noting to save
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminCreateEventVolunteer(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException()).toEvent()
        val volunteers = event.volunteers.toMutableList()
        volunteers.add(EventVolunteer(formData["volunteerLogin"]!!))
        service.save(event.copy(volunteers = volunteers)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }

    suspend fun adminDeleteEventVolunteer(req: ServerRequest): ServerResponse {
        val formData = req.coExtractFormData()
        val event = service.coFindOne(formData["eventId"] ?: throw NotFoundException()).toEvent()
        val volunteers =
            event.volunteers.filter { it.volunteerLogin != formData["volunteerLogin"]!! }
        service.save(event.copy(volunteers = volunteers)).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI/edit/${event.id}")
    }
}
