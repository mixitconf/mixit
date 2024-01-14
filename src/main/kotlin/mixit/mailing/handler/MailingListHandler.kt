package mixit.mailing.handler

import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.event.model.EventService
import mixit.mailing.model.RecipientType
import mixit.mailing.model.RecipientType.Attendee
import mixit.mailing.model.RecipientType.Organization
import mixit.mailing.model.RecipientType.Speaker
import mixit.mailing.model.RecipientType.Sponsor
import mixit.mailing.model.RecipientType.Staff
import mixit.mailing.model.RecipientType.StaffInPause
import mixit.mailing.model.RecipientType.Volunteers
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheTemplate.AdminMailingList
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
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class MailingListHandler(
    private val userService: UserService,
    private val eventService: EventService,
    private val ticketService: TicketService,
    private val talkService: TalkService,
    private val cryptographer: Cryptographer
) {

    suspend fun listMailing(req: ServerRequest): ServerResponse {
        val params = mapOf(
            TITLE to AdminMailingList.title,
            "roles" to RecipientType.values().sorted().map { Pair(it, false) }.toList()
        )
        return ok().renderAndAwait(AdminMailingList.template, params)
    }

    suspend fun generateMailinglist(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val recipientType = formData["recipientType"]?.let { RecipientType.valueOf(it) } ?: Staff
        val params = mapOf(
            TITLE to AdminMailingList.title,
            "roles" to RecipientType.values().sorted().map { Pair(it, it == recipientType) }.toList(),
            "mailinglist" to getMailingList(recipientType)
        )
        return ok().renderAndAwait(AdminMailingList.template, params)
    }

    private suspend fun getMailingList(recipientType: RecipientType): List<MailinglistEntry> =
        when (recipientType) {
            Staff ->
                userService.findByRoles(Role.STAFF).map { staff -> MailinglistEntry(staff) }

            StaffInPause ->
                userService.findByRoles(Role.STAFF_IN_PAUSE).map { staff -> MailinglistEntry(staff) }

            Volunteers ->
                eventService.findByYear(CURRENT_EVENT).volunteers.map { volunteer -> MailinglistEntry(volunteer) }

            Sponsor ->
                eventService.findByYear(CURRENT_EVENT).sponsors.map { sponsor -> MailinglistEntry(sponsor) }

            Speaker ->
                talkService.findByEvent(CURRENT_EVENT)
                    .flatMap { talk -> talk.speakers }
                    .map { speaker -> MailinglistEntry(speaker) }

            Organization ->
                eventService.findByYear(CURRENT_EVENT).organizations.map { sponsor -> MailinglistEntry(sponsor) }

            Attendee ->
                ticketService.findAll().map { attendee -> MailinglistEntry(attendee) }
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
            user.email
        )
    }
}
