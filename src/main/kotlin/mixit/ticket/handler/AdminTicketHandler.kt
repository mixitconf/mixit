package mixit.ticket.handler

import java.time.Instant
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitApplication
import mixit.MixitProperties
import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel.GOLD
import mixit.event.model.SponsorshipLevel.LANYARD
import mixit.event.model.SponsorshipLevel.MIXTEEN
import mixit.event.model.SponsorshipLevel.PARTY
import mixit.event.model.SponsorshipLevel.SILVER
import mixit.features.model.FeatureStateService
import mixit.security.MixitWebFilter
import mixit.security.model.Cryptographer
import mixit.talk.model.TalkService
import mixit.ticket.model.Ticket
import mixit.ticket.model.TicketPronoun
import mixit.ticket.model.TicketService
import mixit.ticket.model.TicketType
import mixit.ticket.model.TicketType.SPEAKER
import mixit.ticket.model.TicketType.SPONSOR_LANYARD
import mixit.ticket.model.TicketType.SPONSOR_MIXTEEN
import mixit.ticket.model.TicketType.SPONSOR_PARTNER
import mixit.ticket.model.TicketType.SPONSOR_PARTY
import mixit.ticket.model.TicketType.SPONSOR_STAND
import mixit.user.model.Role
import mixit.util.enumMatcher
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import mixit.util.mustache.MustacheI18n.CREATION_MODE
import mixit.util.mustache.MustacheI18n.MESSAGE
import mixit.util.mustache.MustacheI18n.PRONOUNS
import mixit.util.mustache.MustacheI18n.TICKET
import mixit.util.mustache.MustacheI18n.TICKETS
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheI18n.TYPES
import mixit.util.mustache.MustacheTemplate.AdminTicket
import mixit.util.mustache.MustacheTemplate.AdminTicketEdit
import mixit.util.mustache.MustacheTemplate.AdminTicketPrint
import mixit.util.mustache.MustacheTemplate.TicketError
import mixit.util.seeOther
import mixit.util.webSession
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.json
import org.springframework.web.reactive.function.server.renderAndAwait
import reactor.core.publisher.Mono

@Component
class AdminTicketHandler(
    private val service: TicketService,
    private val talkService: TalkService,
    private val eventService: EventService,
    private val properties: MixitProperties,
    private val cryptographer: Cryptographer,
    private val featureStateService: FeatureStateService
) {

    companion object {
        const val LIST_URI = "/admin/ticket"
    }

    suspend fun findAll(req: ServerRequest): ServerResponse {
        val tickets = service.findAll().map { it.toEntity(cryptographer) }
        return ok().json().bodyValueAndAwait(tickets)
    }

    suspend fun ticketing(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val type = formData["type"]?.let { TicketType.valueOf(it) }
        val tickets = service.findAll().filter { type == null || it.type == type }.map { it.toDto(cryptographer) }
        val params = mapOf(
            TITLE to AdminTicket.title,
            TICKETS to tickets.sortedBy { it.lastname },
            TYPES to TicketType.entries.map { it to false }
        )
        return ok().renderAndAwait(AdminTicket.template, params)
    }

    suspend fun printTicketing(req: ServerRequest): ServerResponse {
        val tickets = service.findAll()
            .map { ticket ->
                ticket.toDto(cryptographer)
                    .copy(
                        firstname = ticket.firstname?.uppercase() ?: ticket.lastname.uppercase(),
                        lastname = if (ticket.firstname == null) "" else ticket.lastname.uppercase()
                    )
            }
            .sortedBy { it.lastname }
        val params = mapOf(
            TITLE to "admin.ticket.title",
            TICKETS to tickets
        )
        return ok().renderAndAwait(AdminTicketPrint.template, params)
    }

    suspend fun createTicket(req: ServerRequest): ServerResponse =
        this.adminTicket()

    suspend fun editTicket(req: ServerRequest): ServerResponse =
        this.adminTicket(
            service
                .findOneOrNull(req.pathVariable("number"))
                ?.toEntity(cryptographer)
                ?: throw NotFoundException()
        )

    private suspend fun adminTicket(ticket: Ticket = Ticket.empty(cryptographer)): ServerResponse {
        val params = mapOf(
            TITLE to AdminTicketPrint.title,
            CREATION_MODE to ticket.encryptedEmail.isEmpty(),
            TICKET to ticket.decrypt(cryptographer),
            TYPES to enumMatcher(ticket) { ticket.type },
            PRONOUNS to TicketPronoun.entries.map { it to false }
        )
        return ok().renderAndAwait(AdminTicketEdit.template, params)
    }

    suspend fun submit(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val existingTicket = service.findByNumber(formData["number"]!!)
        val ticket = existingTicket
            ?.toEntity(cryptographer)
            ?.copy(
                encryptedEmail = cryptographer.encrypt(formData["email"]!!.lowercase())!!,
                firstname = cryptographer.encrypt(formData["firstname"]),
                lastname = cryptographer.encrypt(formData["lastname"])!!,
                externalId = cryptographer.encrypt(formData["externalId"]),
                type = TicketType.valueOf(formData["type"]!!),
                pronoun = if (formData["pronoun"].isNullOrBlank()) null else TicketPronoun.valueOf(formData["pronoun"]!!),
                englishSpeaker = formData["englishSpeaker"] == "on"
            )
            ?: Ticket(
                number = cryptographer.encrypt(Ticket.generateNewNumber())!!,
                encryptedEmail = cryptographer.encrypt(formData["email"]!!.lowercase())!!,
                firstname = cryptographer.encrypt(formData["firstname"]),
                lastname = cryptographer.encrypt(formData["lastname"])!!,
                externalId = cryptographer.encrypt(formData["externalId"]),
                lotteryRank = formData["lotteryRank"]?.toInt(),
                createdAt = Instant.parse(formData["createdAt"])!!,
                type = TicketType.valueOf(formData["type"]!!),
                pronoun = if (formData["pronoun"].isNullOrBlank()) null else TicketPronoun.valueOf(formData["pronoun"]!!),
                englishSpeaker = formData["englishSpeaker"] == "on"
            )

        val params = mapOf(
            TITLE to TicketError.title,
            MESSAGE to "admin.ticket.error.alreadyexists"
        )

        val result = service.save(ticket)
            .onErrorResume(DuplicateKeyException::class.java) { Mono.empty() }
            .awaitSingleOrNull()

        return result?.let { seeOther("${properties.baseUri}$LIST_URI") } ?: ok().renderAndAwait(
            TicketError.template,
            params
        )
    }

    private suspend fun adminImportStaffOrVolunteer(req: ServerRequest, ticketType: TicketType): ServerResponse {
        // We have to delete all previous users
        service.deleteAll(ticketType)
        this.eventService.findByYear(MixitApplication.CURRENT_EVENT)
            .let {
                if (ticketType == TicketType.VOLUNTEER) it.volunteers else it.organizers
            }
            .map {
                Ticket.empty(cryptographer).copy(
                    encryptedEmail = it.email!!,
                    type = ticketType,
                    firstname = cryptographer.encrypt(it.firstname),
                    lastname = cryptographer.encrypt(it.lastname)!!,
                    login = cryptographer.encrypt(it.login),
                )
            }
            .also { service.save(it) }

        return seeOther("${properties.baseUri}$LIST_URI")
    }

    suspend fun adminImportStaff(req: ServerRequest): ServerResponse =
        adminImportStaffOrVolunteer(req, TicketType.STAFF)

    suspend fun adminImportVolunteers(req: ServerRequest): ServerResponse =
        adminImportStaffOrVolunteer(req, TicketType.VOLUNTEER)

    suspend fun adminImportSponsors(req: ServerRequest): ServerResponse {
        listOf(SPONSOR_LANYARD, SPONSOR_STAND, SPONSOR_MIXTEEN, SPONSOR_PARTNER, SPONSOR_PARTY)
            .onEach { service.deleteAll(it) }

        val levelTypeWithEntries = listOf(GOLD, LANYARD, SILVER, MIXTEEN, PARTY)
        this.eventService.findByYear(MixitApplication.CURRENT_EVENT)
            .sponsors
            .filter { levelTypeWithEntries.contains(it.level) }
            .flatMap { sponsor ->
                val nbOfTicket = when (sponsor.level) {
                    LANYARD -> 9
                    GOLD -> 7
                    PARTY -> 4
                    MIXTEEN -> 1
                    SILVER -> 2
                    else -> throw IllegalArgumentException()
                }
                val type = when (sponsor.level) {
                    LANYARD -> SPONSOR_LANYARD
                    GOLD -> SPONSOR_STAND
                    MIXTEEN -> SPONSOR_MIXTEEN
                    PARTY -> SPONSOR_PARTY
                    SILVER -> SPONSOR_PARTNER
                    else -> throw IllegalArgumentException()
                }
                (1..nbOfTicket).map {
                    Ticket.empty(cryptographer).copy(
                        encryptedEmail = sponsor.email ?: "",
                        type = type,
                        firstname = cryptographer.encrypt("Ticket $it"),
                        lastname = cryptographer.encrypt(sponsor.company)!!,
                        login = cryptographer.encrypt(sponsor.login),
                    )
                }
            }
            .also { service.save(it) }
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    suspend fun adminImportSpeakers(req: ServerRequest): ServerResponse {
        val speakers = talkService.findByEvent(MixitApplication.CURRENT_EVENT).flatMap { it.speakers }
        service.deleteAll(SPEAKER)
        speakers
            .map {
                Ticket.empty(cryptographer).copy(
                    encryptedEmail = it.email!!,
                    type = SPEAKER,
                    firstname = cryptographer.encrypt(it.firstname),
                    lastname = cryptographer.encrypt(it.lastname)!!,
                    login = cryptographer.encrypt(it.login),
                )
            }
            .also { service.save(it) }

        return seeOther("${properties.baseUri}$LIST_URI")
    }

    suspend fun adminDeleteTicket(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        service.deleteOne(cryptographer.encrypt(formData["number"])!!).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI")
    }

    suspend fun showAttendee(req: ServerRequest): ServerResponse {
        val attendee = service.findByNumber(req.pathVariable("number"))
            ?: return seeOther("${properties.baseUri}/")

        val session = req.webSession()
        return when (session.getAttribute<Role>(MixitWebFilter.SESSION_ROLE_KEY)) {
            Role.STAFF -> {
                // A staff member is redirected to Mixette form
                seeOther("${properties.baseUri}/admin/mixette-donation/create/${attendee.number}")
            }

            Role.VOLUNTEER -> {
                // A staff member is redirected to Mixette form
                seeOther("${properties.baseUri}/volunteer/mixette-donation/create/${attendee.number}")
            }

            else -> {
                // Other members could be redirected to user profile in the future
                seeOther("${properties.baseUri}/")
            }
        }
    }
}
