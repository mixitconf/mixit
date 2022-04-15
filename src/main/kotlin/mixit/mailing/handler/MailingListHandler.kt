package mixit.mailing.handler

import mixit.event.handler.AdminEventHandler.Companion.CURRENT_EVENT
import mixit.event.model.EventService
import mixit.mailing.model.RecipientType
import mixit.mailing.model.RecipientType.Attendee
import mixit.mailing.model.RecipientType.Organization
import mixit.mailing.model.RecipientType.Speaker
import mixit.mailing.model.RecipientType.Sponsor
import mixit.mailing.model.RecipientType.Staff
import mixit.mailing.model.RecipientType.StaffInPause
import mixit.mailing.model.RecipientType.Volunteers
import mixit.security.model.Cryptographer
import mixit.talk.model.TalkService
import mixit.ticket.model.CachedTicket
import mixit.ticket.model.TicketService
import mixit.user.model.CachedOrganization
import mixit.user.model.CachedSponsor
import mixit.user.model.CachedStaff
import mixit.user.model.CachedUser
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.util.extractFormData
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class MailingListHandler(
    private val userService: UserService,
    private val eventService: EventService,
    private val ticketService: TicketService,
    private val talkService: TalkService,
    private val cryptographer: Cryptographer
) {

    private enum class MailingPages(val template: String) {
        LIST("admin-mailing-list")
    }

    fun listMailing(req: ServerRequest) =
        ok().render(
            MailingPages.LIST.template,
            mapOf(
                Pair("title", "mailinglist.title"),
                Pair("roles", RecipientType.values().sorted().map { Pair(it, false) }.toList())
            )
        )

    fun generateMailinglist(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            val recipientType = formData["recipientType"]?.let { RecipientType.valueOf(it) } ?: Staff
            ok().render(
                MailingPages.LIST.template,
                mapOf(
                    Pair("title", "mailinglist.title"),
                    Pair("roles", RecipientType.values().sorted().map { Pair(it, it == recipientType) }.toList()),
                    Pair("mailinglist", getMailingList(recipientType))
                )
            )
        }

    private fun getMailingList(recipientType: RecipientType): Mono<List<MailinglistEntry>> =
        when (recipientType) {
            Staff ->
                userService.findByRoles(Role.STAFF).map { it.map { staff -> MailinglistEntry(staff) } }
            StaffInPause ->
                userService.findByRoles(Role.STAFF_IN_PAUSE).map { it.map { staff -> MailinglistEntry(staff) } }
            Volunteers ->
                eventService.findByYear(CURRENT_EVENT.toInt())
                    .map { it.volunteers.map { volunteer -> MailinglistEntry(volunteer) } }
            Sponsor ->
                eventService.findByYear(CURRENT_EVENT.toInt())
                    .map { it.sponsors.map { sponsor -> MailinglistEntry(sponsor) } }
            Speaker ->
                talkService.findByEvent(CURRENT_EVENT)
                    .map { it.flatMap { talk -> talk.speakers }.map { speaker -> MailinglistEntry(speaker) } }
            Organization ->
                eventService.findByYear(CURRENT_EVENT.toInt())
                    .map { it.organizations.map { sponsor -> MailinglistEntry(sponsor) } }
            Attendee ->
                ticketService.findAll().map { it.map { attendee -> MailinglistEntry(attendee) } }
            // TODO add user who have subscribed to our newletter
        }


    inner class MailinglistEntry(val firstname: String?, val lastname: String, val email: String) {
        constructor(user: CachedUser) : this(
            user.firstname,
            user.lastname,
            user.email?.let { cryptographer.decrypt(it) } ?: "unknown"
        )

        constructor(user: CachedStaff) : this(
            user.firstname,
            user.lastname,
            user.email?.let { cryptographer.decrypt(it) } ?: "unknown"
        )

        constructor(user: CachedSponsor) : this(
            "",
            user.company,
            user.email?.let { cryptographer.decrypt(it) } ?: "unknown"
        )

        constructor(user: CachedOrganization) : this(
            "",
            user.company,
            user.email?.let { cryptographer.decrypt(it) } ?: "unknown"
        )

        constructor(user: User) : this(
            user.firstname,
            user.lastname,
            user.email?.let { cryptographer.decrypt(it) } ?: "unknown"
        )

        constructor(user: CachedTicket) : this(
            user.firstname,
            user.lastname,
            user.encryptedEmail.let { cryptographer.decrypt(it) } ?: "unknown"
        )
    }
}
