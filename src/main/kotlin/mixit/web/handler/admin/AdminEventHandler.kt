package mixit.web.handler.admin

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import mixit.MixitProperties
import mixit.model.Event
import mixit.model.EventOrganization
import mixit.model.EventSponsoring
import mixit.model.EventVolunteer
import mixit.model.SponsorshipLevel
import mixit.repository.EventRepository
import mixit.util.enumMatcher
import mixit.util.extractFormData
import mixit.util.seeOther
import mixit.web.handler.admin.AdminUtils.toJson
import mixit.web.handler.admin.AdminUtils.toLink
import mixit.web.handler.admin.AdminUtils.toLinks
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AdminEventHandler(
    private val eventRepository: EventRepository,
    private val properties: MixitProperties,
    private val objectMapper: ObjectMapper
) {

    companion object {
        const val TEMPLATE_LIST = "admin-events"
        const val TEMPLATE_EDIT = "admin-event"
        const val TEMPLATE_SPONSOR = "admin-event-sponsor"
        const val TEMPLATE_ORGANIZATION = "admin-event-organization"
        const val TEMPLATE_VOLUNTEER = "admin-event-volunteer"
        const val LIST_URI = "/admin/events"
        const val CURRENT_EVENT = "2022"
    }

    fun adminEvents(req: ServerRequest) =
        ok().render(
            TEMPLATE_LIST,
            mapOf(Pair("events", eventRepository.findAll()), Pair("title", "admin.events.title"))
        )

    fun createEvent(req: ServerRequest): Mono<ServerResponse> =
        this.adminEvent()

    fun editEvent(req: ServerRequest): Mono<ServerResponse> =
        eventRepository.findOne(req.pathVariable("eventId")).flatMap { this.adminEvent(it) }

    private fun adminEvent(event: Event = Event()) =
        ok().render(
            TEMPLATE_EDIT,
            mapOf(
                Pair("creationMode", event.id.isEmpty()),
                Pair("event", event),
                Pair("links", event.photoUrls.toJson(objectMapper)),
                Pair("videolink", event.videoUrl?.toJson(objectMapper) ?: "")
            )
        )

    fun adminSaveEvent(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap { event ->
                    eventRepository.save(
                        Event(
                            event.id,
                            LocalDate.parse(formData["start"]!!),
                            LocalDate.parse(formData["end"]!!),
                            formData["current"]?.toBoolean() ?: false,
                            event.sponsors,
                            event.organizations,
                            event.volunteers,
                            formData["photoUrls"]?.toLinks(objectMapper) ?: emptyList(),
                            formData["videoUrl"]?.toLink(objectMapper),
                            formData["schedulingFileUrl"]
                        )
                    ).then(seeOther("${properties.baseUri}$LIST_URI"))
                }
                .switchIfEmpty(
                    eventRepository.save(
                        Event(
                            formData["eventId"]!!,
                            LocalDate.parse(formData["start"]!!),
                            LocalDate.parse(formData["end"]!!),
                            formData["current"]?.toBoolean() ?: false
                        )
                    ).then(seeOther("${properties.baseUri}$LIST_URI"))
                )
        }


    fun createEventSponsoring(req: ServerRequest): Mono<ServerResponse> =
        adminEventSponsoring(req.pathVariable("eventId"))


    private fun adminEventSponsoring(eventId: String, eventSponsoring: EventSponsoring = EventSponsoring()) =
        ok().render(
            TEMPLATE_SPONSOR,
            mapOf(
                Pair("creationMode", eventSponsoring.sponsorId == ""),
                Pair("eventId", eventId),
                Pair("eventSponsoring", eventSponsoring),
                Pair("levels", enumMatcher(eventSponsoring) { it?.level ?: SponsorshipLevel.NONE })
            )
        )

    fun adminUpdateEventSponsoring(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap { event ->
                    eventRepository
                        .save(event.copy(sponsors = event.sponsors.map { sponsoring ->
                            if (sponsoring.sponsorId == formData["sponsorId"] && sponsoring.level.name == formData["level"]) {
                                EventSponsoring(
                                    sponsoring.level,
                                    sponsoring.sponsorId,
                                    formData["subscriptionDate"]?.let { LocalDate.parse(it) } ?: LocalDate.now()
                                )
                            } else {
                                sponsoring
                            }
                        }))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun adminCreateEventSponsoring(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap { event ->
                    // We create a mutable list
                    val sponsors = event.sponsors.toMutableList()
                    sponsors.add(
                        EventSponsoring(
                            SponsorshipLevel.valueOf(formData["level"]!!),
                            formData["sponsorId"]!!,
                            formData["subscriptionDate"]?.let { LocalDate.parse(it) } ?: LocalDate.now()
                        )
                    )
                    eventRepository.save(event.copy(sponsors = sponsors))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun adminDeleteEventSponsoring(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap { event ->
                    // We create a mutable list
                    eventRepository.save(
                        event.copy(sponsors = event.sponsors
                            .filterNot { it.sponsorId == formData["sponsorId"] && it.level.name == formData["level"] })
                    )
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun editEventSponsoring(req: ServerRequest): Mono<ServerResponse> =
        eventRepository
            .findOne(req.pathVariable("eventId"))
            .flatMap { event ->
                adminEventSponsoring(
                    req.pathVariable("eventId"),
                    event.sponsors
                        .first {
                            it.sponsorId == req.pathVariable("sponsorId")
                                    && it.level.name == req.pathVariable("level")
                        }
                )
            }

    fun editEventOrganization(req: ServerRequest): Mono<ServerResponse> =
        req.pathVariable("eventId").let { eventId ->
            eventRepository
                .findOne(eventId)
                .flatMap { event ->
                    adminEventOrganization(
                        eventId,
                        event.organizations.first { it.organizationLogin == req.pathVariable("organizationLogin") }
                    )
                }
        }

    fun createEventOrganization(req: ServerRequest): Mono<ServerResponse> =
        adminEventOrganization(req.pathVariable("eventId"))

    private fun adminEventOrganization(eventId: String, eventOrganization: EventOrganization? = null) = ok()
        .render(
            TEMPLATE_ORGANIZATION,
            mapOf(
                Pair("creationMode", eventOrganization == null),
                Pair("eventId", eventId),
                Pair("eventOrganization", eventOrganization)
            )
        )

    fun adminUpdateEventOrganization(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap {
                    // For the moment we have noting to save
                    seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}")
                }
        }

    fun adminCreateEventOrganization(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap { event ->
                    val organizations = event.organizations.toMutableList()
                    organizations.add(EventOrganization(formData["organizationLogin"]!!))
                    eventRepository.save(event.copy(organizations = organizations))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun adminDeleteEventOrganization(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap { event ->
                    val organizations =
                        event.organizations.filter { it.organizationLogin != formData["organizationLogin"]!! }
                    eventRepository.save(event.copy(organizations = organizations))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun editEventVolunteer(req: ServerRequest): Mono<ServerResponse> =
        req.pathVariable("eventId").let { eventId ->
            eventRepository
                .findOne(eventId)
                .flatMap { event ->
                    adminEventVolunteer(
                        eventId,
                        event.volunteers.first { it.volunteerLogin == req.pathVariable("volunteerLogin") }
                    )
                }
        }

    fun createEventVolunteer(req: ServerRequest): Mono<ServerResponse> =
        adminEventVolunteer(req.pathVariable("eventId"))

    private fun adminEventVolunteer(eventId: String, eventVolunteer: EventVolunteer? = null) = ok()
        .render(
            TEMPLATE_VOLUNTEER,
            mapOf(
                Pair("creationMode", eventVolunteer == null),
                Pair("eventId", eventId),
                Pair("eventOrganization", eventVolunteer)
            )
        )

    fun adminUpdateEventVolunteer(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap {
                    // For the moment we have noting to save
                    seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}")
                }
        }

    fun adminCreateEventVolunteer(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap { event ->
                    val volunteers = event.volunteers.toMutableList()
                    volunteers.add(EventVolunteer(formData["volunteerLogin"]!!))
                    eventRepository.save(event.copy(volunteers = volunteers))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun adminDeleteEventVolunteer(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap { event ->
                    val volunteers =
                        event.volunteers.filter { it.volunteerLogin != formData["volunteerLogin"]!! }
                    eventRepository.save(event.copy(volunteers = volunteers))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }
}
