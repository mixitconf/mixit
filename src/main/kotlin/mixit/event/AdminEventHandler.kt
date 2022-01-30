package mixit.event

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import mixit.MixitProperties
import mixit.event.model.Event
import mixit.event.model.EventOrganization
import mixit.event.model.EventSponsoring
import mixit.event.model.EventVolunteer
import mixit.event.model.SponsorshipLevel
import mixit.util.AdminUtils.toJson
import mixit.util.AdminUtils.toLink
import mixit.util.AdminUtils.toLinks
import mixit.util.enumMatcher
import mixit.util.extractFormData
import mixit.util.json
import mixit.util.seeOther
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

@Component
class AdminEventHandler(
    private val service: EventService,
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

    fun adminEvents(req: ServerRequest): Mono<ServerResponse> {
        val events = service.findAll().sort(Comparator.comparing { it.id }).map { it.toEvent() }
        return ok().render(
            TEMPLATE_LIST,
            mapOf(Pair("events", events), Pair("title", "admin.events.title"))
        )
    }

    fun createEvent(req: ServerRequest): Mono<ServerResponse> =
        this.adminEvent()

    fun editEvent(req: ServerRequest): Mono<ServerResponse> =
        service.findOne(req.pathVariable("eventId")).flatMap { this.adminEvent(it.toEvent()) }

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
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
                .flatMap { event ->
                    service.save(
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
                    service.save(
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
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
                .flatMap { event ->
                    service
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
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
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
                    service.save(event.copy(sponsors = sponsors))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun adminDeleteEventSponsoring(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
                .flatMap { event ->
                    // We create a mutable list
                    service.save(
                        event.copy(sponsors = event.sponsors
                            .filterNot { it.sponsorId == formData["sponsorId"] && it.level.name == formData["level"] })
                    )
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun editEventSponsoring(req: ServerRequest): Mono<ServerResponse> =
        service
            .findOne(req.pathVariable("eventId"))
            .map { it.toEvent() }
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
            service
                .findOne(eventId)
                .map { it.toEvent() }
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
            service
                .findOne(formData["eventId"]!!)
                .flatMap {
                    // For the moment we have noting to save
                    seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}")
                }
        }

    fun adminCreateEventOrganization(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
                .flatMap { event ->
                    val organizations = event.organizations.toMutableList()
                    organizations.add(EventOrganization(formData["organizationLogin"]!!))
                    service.save(event.copy(organizations = organizations))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun adminDeleteEventOrganization(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
                .flatMap { event ->
                    val organizations =
                        event.organizations.filter { it.organizationLogin != formData["organizationLogin"]!! }
                    service.save(event.copy(organizations = organizations))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun editEventVolunteer(req: ServerRequest): Mono<ServerResponse> =
        req.pathVariable("eventId").let { eventId ->
            service
                .findOne(eventId)
                .map { it.toEvent() }
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
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
                .flatMap {
                    // For the moment we have noting to save
                    seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}")
                }
        }

    fun adminCreateEventVolunteer(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
                .flatMap { event ->
                    val volunteers = event.volunteers.toMutableList()
                    volunteers.add(EventVolunteer(formData["volunteerLogin"]!!))
                    service.save(event.copy(volunteers = volunteers))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun adminDeleteEventVolunteer(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            // We need to find the event in database
            service
                .findOne(formData["eventId"]!!)
                .map { it.toEvent() }
                .flatMap { event ->
                    val volunteers =
                        event.volunteers.filter { it.volunteerLogin != formData["volunteerLogin"]!! }
                    service.save(event.copy(volunteers = volunteers))
                        .then(seeOther("${properties.baseUri}$LIST_URI/edit/${formData["eventId"]!!}"))
                }
        }

    fun cacheStats(req: ServerRequest) =
        ok().json().body(Mono.justOrEmpty(service.cacheStats()))

    fun invalidateCache(req: ServerRequest): Mono<ServerResponse> {
        service.invalidateCache()
        return seeOther("${properties.baseUri}$LIST_URI")
    }

}
