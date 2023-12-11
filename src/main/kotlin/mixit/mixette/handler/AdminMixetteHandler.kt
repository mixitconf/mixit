package mixit.mixette.handler

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitProperties
import mixit.event.model.EventService
import mixit.mixette.model.MixetteDonation
import mixit.mixette.repository.MixetteDonationRepository
import mixit.routes.MustacheI18n.CREATION_MODE
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate.AdminMixetteDonation
import mixit.routes.MustacheTemplate.AdminMixetteDonor
import mixit.routes.MustacheTemplate.AdminMixetteDonors
import mixit.routes.MustacheTemplate.AdminMixetteOrganization
import mixit.routes.MustacheTemplate.AdminMixetteOrganizations
import mixit.security.MixitWebFilter.Companion.SESSION_LOGIN_KEY
import mixit.security.MixitWebFilter.Companion.SESSION_ROLE_KEY
import mixit.security.model.Cryptographer
import mixit.ticket.model.TicketService
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.util.errors.NotFoundException
import mixit.util.extractFormData
import org.springframework.web.reactive.function.server.json
import mixit.util.seeOther
import mixit.util.toNumber
import mixit.util.webSession
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.queryParamOrNull
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class AdminMixetteHandler(
    private val repository: MixetteDonationRepository,
    private val userService: UserService,
    private val service: EventService,
    private val ticketService: TicketService,
    private val properties: MixitProperties,
    private val cryptographer: Cryptographer
) {

    companion object {
        const val LIST_URI_FOR_ADMIN = "/admin/mixette-organization"
        const val LIST_URI_FOR_VOLUNTEER = "/volunteer/mixette-organization"
    }

    suspend fun findAll(req: ServerRequest) = ok().json().bodyValueAndAwait(repository.findAll())

    /**
     * Used to display aggregated data by donors
     */
    suspend fun adminDonorDonations(req: ServerRequest): ServerResponse =
        adminGroupDonations(AdminMixetteDonors.template) {
            findDonorByEncryptedTicketNumber(it.encryptedTicketNumber!!)
                ?: MixetteUserDonationDto(
                    name = "",
                    login = it.userLogin,
                    email = cryptographer.decrypt(it.encryptedUserEmail)!!,
                    ticketNumber = cryptographer.decrypt(it.encryptedTicketNumber)
                )
        }

    /**
     * Used to display aggregated data by organizations
     */
    suspend fun adminOrganizationDonations(req: ServerRequest): ServerResponse =
        adminGroupDonations(AdminMixetteOrganizations.template) { donation ->
            val user = userService.findOneOrNull(donation.organizationLogin) ?: throw NotFoundException()
            MixetteOrganizationDonationDto(name = user.organizationName, login = user.login)
        }

    private suspend fun <T : MixetteDonationDto> adminGroupDonations(
        target: String,
        transformation: suspend (MixetteDonation) -> T
    ): ServerResponse {
        val donations = repository.findAllByYear(CURRENT_EVENT)
        val donationByOrgas = donations
            .groupBy { transformation.invoke(it) }
            .map { entry ->
                entry.key.populate(
                    number = entry.value.size,
                    quantity = entry.value.sumOf { it.quantity },
                    amount = entry.value.sumOf { it.quantity * properties.mixetteValue.toDouble() }
                )
            }
            .sortedByDescending { it.quantity }
            .mapIndexed { index, donation -> donation.updateRank(index + 1) }

        return ok().renderAndAwait(target, mapOf("donations" to donationByOrgas, TITLE to "admin.donations.title"))
    }

    /**
     * Used to display the page to create a new donation
     */
    suspend fun addDonation(req: ServerRequest): ServerResponse =
        req.webSession().let {
            this.adminDonation(MixetteDonation(CURRENT_EVENT, userLogin = it.getAttribute(SESSION_LOGIN_KEY)))
        }

    /**
     * Used to display the page to create a new donation, when a volunteer or a staff member has scanned a badge
     */
    suspend fun addDonationForAttendee(req: ServerRequest): ServerResponse {
        val donor = ticketService.findByNumber(req.pathVariable("number")) ?: throw NotFoundException()
        return req
            .webSession()
            .let {
                this.adminDonation(
                    MixetteDonation(
                        year = CURRENT_EVENT,
                        encryptedTicketNumber = cryptographer.encrypt(donor.number),
                        userLogin = donor.login,
                        createdBy = it.getAttribute(SESSION_LOGIN_KEY),
                        encryptedUserEmail = cryptographer.encrypt(donor.email)!!
                    )
                )
            }
    }

    /**
     * Used to display screen to edit a donation
     */
    suspend fun editDonation(req: ServerRequest): ServerResponse =
        this.adminDonation(repository.findOneOrNull(req.pathVariable("id")) ?: throw NotFoundException())

    /**
     * Used to display aggregated data for an organization
     */
    suspend fun editOrganization(req: ServerRequest): ServerResponse {
        val login = req.queryParamOrNull("login") ?: throw NotFoundException()
        val donations = repository.findByOrganizationLogin(login, CURRENT_EVENT)

        return this.adminGroupDonation(AdminMixetteOrganization.template, donations) {
            val organization = userService.findOneOrNull(it.organizationLogin) ?: throw NotFoundException()
            MixetteOrganizationDonationDto(
                name = organization.company ?: "${organization.firstname} ${organization.lastname}",
                login = organization.login
            )
        }
    }

    /**
     * Used to display aggregated data for a donor
     */
    suspend fun editDonor(req: ServerRequest): ServerResponse {
        val ticketNumber = req.queryParamOrNull("ticketNumber") ?: throw NotFoundException()
        val donations = repository.findByTicketNumber(cryptographer.encrypt(ticketNumber)!!, CURRENT_EVENT)
        return this.adminGroupDonation(AdminMixetteDonor.template, donations) {
            findDonorByEncryptedTicketNumber(it.encryptedTicketNumber!!) ?: throw NotFoundException()
        }
    }

    private suspend fun <T : MixetteDonationDto> adminGroupDonation(
        target: String,
        donations: List<MixetteDonation>,
        transformation: suspend (MixetteDonation) -> T
    ): ServerResponse {
        val quantity = donations.sumOf { it.quantity }
        val firstDonation = transformation.invoke(donations.first())

        val userDonation = firstDonation.populate(
            number = donations.size,
            quantity = quantity,
            amount = quantity * properties.mixetteValue.toDouble()
        )

        val dtos = donations.map {
            MixetteUserDonationDto(
                name = "",
                email = cryptographer.decrypt(it.encryptedUserEmail)!!,
                ticketNumber = cryptographer.decrypt(it.encryptedTicketNumber),
                login = if (target == AdminMixetteDonor.template) it.organizationLogin else it.userLogin,
                id = it.id,
                quantity = it.quantity
            )
        }

        val params = mapOf(
            TITLE to AdminMixetteDonor.title, "donations" to dtos, "userDonation" to userDonation
        )
        return ok().renderAndAwait(target, params)
    }

    private suspend fun adminDonation(
        donation: MixetteDonation,
        errors: Map<String, String> = emptyMap()
    ): ServerResponse {
        val event = service.findByYear(CURRENT_EVENT)
        val donor = findDonorByEncryptedTicketNumber(donation.encryptedTicketNumber ?: "")
        val organization = userService.findOneOrNull(donation.organizationLogin)

        val params = mapOf(
            CREATION_MODE to (donation.id == null),
            "donation" to MixetteDonationDetailedDto(
                donation = donation,
                cryptographer = cryptographer,
                username = donor?.name,
                organizationName = organization?.organizationName
            ),
            "organizations" to event.organizations.map { MixetteOrganizationDto(it, donation) },
            "errors" to errors,
            "hasErrors" to errors.isNotEmpty()
        )

        return ok().renderAndAwait(AdminMixetteDonation.template, params)
    }

    private suspend fun persistDonation(
        req: ServerRequest,
        donation: MixetteDonation,
        donor: MixetteUserDonationDto,
        receiver: User,
        quantity: Int?,
        errors: Map<String, String>,
    ): ServerResponse {
        val newDonation = donation.copy(
            encryptedTicketNumber = cryptographer.encrypt(donor.ticketNumber)!!,
            userLogin = donor.login,
            encryptedUserEmail = cryptographer.encrypt(donor.email)!!,
            organizationLogin = receiver.login,
            quantity = quantity ?: 0
        )
        if (errors.isNotEmpty()) {
            return adminDonation(newDonation, errors)
        }
        val session = req.webSession()
        val connectedUser = session.getAttribute<String>(SESSION_LOGIN_KEY)
        val userRole = session.getAttribute<Role>(SESSION_ROLE_KEY)

        if (newDonation.id == null) {
            repository.insert(newDonation.copy(createdBy = connectedUser)).awaitSingle()
        } else {
            repository.update(newDonation.copy(updatedBy = connectedUser)).awaitSingle()
        }

        return seeOther("${properties.baseUri}${if (userRole == Role.STAFF) LIST_URI_FOR_ADMIN else LIST_URI_FOR_VOLUNTEER}")
    }

    /**
     * This function parse users and tickets to find people
     */
    private suspend fun findDonorByEncryptedTicketNumber(ticketNumber: String): MixetteUserDonationDto? {
        // We find the ticket
        val ticket = ticketService.findByNumber(cryptographer.decrypt(ticketNumber)!!)?.toEntity(cryptographer)
        // We try to find if user is known
        val user = ticket?.let { userService.findOneByEncryptedEmailOrNull(it.encryptedEmail) }?.toUser()

        if (ticket == null) {
            return null
        }
        val name = ticket.let { "${cryptographer.decrypt(it.firstname)} ${cryptographer.decrypt(it.lastname)}" }

        return MixetteUserDonationDto(
            name = name,
            email = cryptographer.decrypt(ticket.encryptedEmail)!!,
            ticketNumber = cryptographer.decrypt(ticket.number),
            login = user?.login
        )
    }

    suspend fun adminSaveDonation(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val organizationLogin: String = formData["organizationLogin"]!!
        val ticketNumber: String = cryptographer.encrypt(formData["ticketNumber"])!!

        val receiver = userService.findOneOrNull(organizationLogin)
        val donor = findDonorByEncryptedTicketNumber(ticketNumber) ?: throw NotFoundException()

        val errors = mutableMapOf<String, String>()

//        if (donor.login.isNullOrEmpty() || donor.ticketNumber.isNullOrEmpty()) {
//            errors["userLogin"] = "admin.donations.error.userLogin.required"
//        }
        if (receiver == null) {
            errors["organizationLogin"] = "admin.donations.error.organizationLogin.required"
        }
        val quantity = formData["quantity"]?.toNumber() ?: 0
        if (quantity <= 0) {
            errors["quantity"] = "admin.donations.error.quantity.invalid"
        }

        val donation = repository.findOneOrNull(formData["id"] ?: "") ?: MixetteDonation(CURRENT_EVENT)
        return persistDonation(
            req = req,
            donation = donation,
            donor = donor.copy(email = donor.email),
            receiver = receiver?.toUser() ?: User(organizationLogin),
            quantity = quantity,
            errors = errors
        )
    }

    suspend fun adminDeleteDonation(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        repository.deleteOne(formData["id"]!!).awaitSingleOrNull()
        return seeOther("${properties.baseUri}$LIST_URI_FOR_ADMIN")
    }
}
