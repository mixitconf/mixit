package mixit.mailing.handler

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitProperties
import mixit.mailing.model.Mailing
import mixit.mailing.model.RecipientType
import mixit.mailing.model.RecipientType.Organization
import mixit.mailing.model.RecipientType.Speaker
import mixit.mailing.model.RecipientType.Sponsor
import mixit.mailing.model.RecipientType.Staff
import mixit.mailing.model.RecipientType.StaffInPause
import mixit.mailing.model.RecipientType.Volunteers
import mixit.mailing.repository.MailingRepository
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate.AdminMailingConfirmation
import mixit.routes.MustacheTemplate.AdminMailingEdit
import mixit.routes.MustacheTemplate.AdminMailingList
import mixit.routes.MustacheTemplate.EmailMailing
import mixit.security.model.Cryptographer
import mixit.user.model.CachedUser
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.util.email.EmailService
import mixit.util.enumMatcher
import mixit.util.extractFormData
import mixit.util.seeOther
import mixit.util.toHTML
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
import java.time.LocalDateTime
import java.util.Locale

@Component
class MailingHandler(
    private val userService: UserService,
    private val cryptographer: Cryptographer,
    private val mailingRepository: MailingRepository,
    private val properties: MixitProperties,
    private val emailService: EmailService
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    suspend fun listMailing(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(
            AdminMailingList.template,
            mapOf(TITLE to "mailing.title", "mailings" to mailingRepository.findAll())
        )

    suspend fun createMailing(req: ServerRequest): ServerResponse =
        this.displayMailing()

    suspend fun editMailing(req: ServerRequest): ServerResponse {
        val mailing = mailingRepository.findOne(req.pathVariable("id"))
        return displayMailing(mailing)
    }

    private suspend fun displayMailing(mailing: Mailing? = null): ServerResponse =
        ok().renderAndAwait(
            AdminMailingEdit.template,
            mapOf(
                TITLE to "mailing.title",
                "roles" to enumMatcher(mailing) { mailing?.type ?: Staff },
                "mailing" to (mailing ?: Mailing()),
                "recipientLogins" to (mailing?.recipientLogins?.joinToString() ?: emptyList<String>())
            )
        )

    suspend fun deleteMailing(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        mailingRepository.deleteOne(formData["id"]!!).awaitSingleOrNull()
        return seeOther("${properties.baseUri}/admin/mailings")
    }

    suspend fun previewMailing(req: ServerRequest): ServerResponse {
        val mailing = persistMailing(req)
        val params = mapOf(
            "user" to User().copy(firstname = "Bot"),
            "message" to mailing.content.toHTML()
        )
        return ok().renderAndAwait(EmailMailing.template, params)
    }

    private suspend fun getUsers(mailing: Mailing): List<CachedUser> {
        if (mailing.recipientLogins.isNotEmpty()) {
            return userService.findAllByIds(mailing.recipientLogins)
        }
        if (mailing.type != null) {
            return when (mailing.type) {
                Staff -> userService.findByRoles(Role.STAFF)
                StaffInPause -> userService.findByRoles(Role.STAFF_IN_PAUSE)
                Volunteers -> userService.findByRoles(Role.VOLUNTEER)
                RecipientType.Attendee, Sponsor, Organization, Speaker -> emptyList()
            }
        }
        return emptyList()
    }

    private suspend fun persistMailing(req: ServerRequest): Mailing {
        val formData = req.extractFormData()
        return mailingRepository
            .save(
                Mailing(
                    id = formData["id"],
                    addedAt = LocalDateTime.parse(formData["addedAt"]),
                    type = formData["recipientType"]?.let { RecipientType.valueOf(it) },
                    title = formData["title"]!!,
                    content = formData["content"]!!,
                    recipientLogins = formData["recipientLogins"]?.split(",") ?: emptyList()
                )
            ).awaitSingle()
    }

    suspend fun sendMailing(req: ServerRequest): ServerResponse {
        val mailing = persistMailing(req).let {
            val users = getUsers(it)
            MailingDto(it.title, it.content, users.map { user -> user.toUser() })
        }
        mailing.users.forEach { user ->
            val email = cryptographer.decrypt(user.email)
            try {
                logger.info("Send a mailing to $email")
                emailService.send(
                    EmailMailing.template,
                    user,
                    Locale.FRANCE,
                    mailing.title,
                    mapOf(Pair("message", mailing.content.toHTML()))
                )
            } catch (e: Exception) {
                logger.error("Error on mailing sent to $email", e)
            }
        }
        return ok()
            .renderAndAwait(
                AdminMailingConfirmation.template,
                mapOf(
                    "emails" to mailing.users.mapNotNull { cryptographer.decrypt(it.email) }
                )
            )
    }

    suspend fun saveMailing(req: ServerRequest): ServerResponse {
        persistMailing(req)
        return seeOther("${properties.baseUri}/admin/mailings")
    }
}
