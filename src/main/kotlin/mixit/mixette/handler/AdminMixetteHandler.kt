package mixit.mixette.handler

import mixit.MixitProperties
import mixit.event.handler.AdminEventHandler.Companion.CURRENT_EVENT
import mixit.event.model.EventService
import mixit.mixette.model.MixetteDonation
import mixit.mixette.repository.MixetteDonationRepository
import mixit.security.model.Cryptographer
import mixit.ticket.model.FinalTicket
import mixit.ticket.model.TicketService
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.util.extractFormData
import mixit.util.json
import mixit.util.seeOther
import mixit.util.toNumber
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.queryParamOrNull
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

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
        const val TEMPLATE_BY_USER_LIST = "admin-mixette-donors"
        const val TEMPLATE_BY_ORGA_LIST = "admin-mixette-organizations"
        const val TEMPLATE_BY_USER = "admin-mixette-donor"
        const val TEMPLATE_BY_ORGA = "admin-mixette-organization"
        const val TEMPLATE_EDIT = "admin-mixette-donation"
        const val LIST_URI = "/admin/mixette-organization"
    }

    fun findAll(req: ServerRequest) = ok().json().body(repository.findAll())

    fun adminDonorDonations(req: ServerRequest): Mono<ServerResponse> =
        adminGroupDonations(TEMPLATE_BY_USER_LIST) {
            MixetteUserDonationDto(
                name = it.username,
                email = cryptographer.decrypt(it.userEmail)!!,
                ticketNumber = it.ticketNumber,
                login = it.userLogin
            )
        }

    fun adminOrganizationDonations(req: ServerRequest): Mono<ServerResponse> =
        adminGroupDonations(TEMPLATE_BY_ORGA_LIST) {
            MixetteOrganizationDonationDto(
                name = it.organizationName,
                login = it.organizationLogin
            )
        }

    private fun adminGroupDonations(
        target: String,
        transformation: (MixetteDonation) -> MixetteDonationDto
    ): Mono<ServerResponse> {
        val donationByOrgas = repository.findAllByYear(CURRENT_EVENT).collectList().map { donations ->
            donations
                .groupBy { transformation.invoke(it) }
                .map { entry ->
                    entry.key.populate(
                        number = entry.value.size,
                        quantity = entry.value.sumOf { it.quantity },
                        amount = entry.value.sumOf { it.quantity * properties.mixetteValue.toDouble() }
                    )
                }
        }
        return ok().render(
            target,
            mapOf(Pair("donations", donationByOrgas), Pair("title", "admin.donations.title"))
        )
    }

    fun addDonation(req: ServerRequest): Mono<ServerResponse> =
        this.adminDonation()

    fun addDonationForAttendee(req: ServerRequest): Mono<ServerResponse> =
        ticketService
            .findByNumber(req.pathVariable("number"))
            .flatMap { donor ->
                this.adminDonation(
                    MixetteDonation(
                        year = CURRENT_EVENT,
                        ticketNumber = donor.number,
                        userLogin = donor.login,
                        username = "${donor.firstname} ${donor.lastname}",
                        userEmail = cryptographer.encrypt(donor.email)!!
                    )
                )
            }

    fun editDonation(req: ServerRequest): Mono<ServerResponse> =
        repository.findOne(req.pathVariable("id")).flatMap { this.adminDonation(it) }

    fun editOrga(req: ServerRequest): Mono<ServerResponse> =
        this.adminGroupDonation(
            TEMPLATE_BY_ORGA,
            repository.findByOrganizationLogin(req.queryParamOrNull("login")!!, CURRENT_EVENT).collectList()
        ) {
            MixetteOrganizationDonationDto(name = it.organizationName, login = it.organizationLogin)
        }

    fun editDonor(req: ServerRequest): Mono<ServerResponse> =
        this.adminGroupDonation(
            TEMPLATE_BY_USER,
            repository.findByEmail(cryptographer.encrypt(req.queryParamOrNull("email"))!!, CURRENT_EVENT).collectList()
        ) {
            MixetteUserDonationDto(
                name = it.username,
                ticketNumber = it.ticketNumber,
                login = it.userLogin,
                email = cryptographer.decrypt(it.userEmail)!!
            )
        }

    private fun adminGroupDonation(
        target: String,
        monoDonations: Mono<List<MixetteDonation>>,
        transformation: (MixetteDonation) -> MixetteDonationDto
    ): Mono<ServerResponse> =
        monoDonations.flatMap { donations ->
            val quantity = donations.sumOf { it.quantity }
            val userDonation = transformation.invoke(donations.first()).populate(
                number = donations.size,
                quantity = quantity,
                amount = quantity * properties.mixetteValue.toDouble()
            )
            ok().render(
                target,
                mapOf(
                    Pair("donations", donations),
                    Pair("userDonation", userDonation),
                    Pair("title", "admin.donations.title")
                )
            )
        }

    private fun adminDonation(
        donation: MixetteDonation = MixetteDonation(CURRENT_EVENT),
        errors: Map<String, String> = emptyMap()
    ): Mono<ServerResponse> =
        service.findByYear(CURRENT_EVENT.toInt())
            .flatMap { event ->
                ok().render(
                    TEMPLATE_EDIT,
                    mapOf(
                        Pair("creationMode", donation.id == null),
                        Pair("donation", donation.copy(userEmail = cryptographer.decrypt(donation.userEmail)!!)),
                        Pair("organizations", event.organizations.map { MixetteOrganizationDto(it, donation) }),
                        Pair("errors", errors),
                        Pair("hasErrors", errors.isNotEmpty())
                    )
                )
            }

    private fun persistDonation(
        donation: MixetteDonation,
        donor: MixetteUserDonationDto,
        receiver: User,
        quantity: Int?,
        errors: Map<String, String>
    ): Mono<ServerResponse> {
        if (errors.isNotEmpty()) {
            return adminDonation(donation, errors)
        }
        val newDonation = donation.copy(
            ticketNumber = donor.ticketNumber,
            userLogin = donor.login,
            username = donor.name,
            userEmail = donor.email,
            organizationLogin = receiver.login ?: "",
            organizationName = receiver.company ?: "${receiver.firstname} ${receiver.lastname}",
            quantity = quantity ?: 0
        )

        if (newDonation.id == null) {
            return repository.insert(newDonation).flatMap { seeOther("${properties.baseUri}$LIST_URI") }
        }
        return repository.update(newDonation).flatMap { seeOther("${properties.baseUri}$LIST_URI") }
    }

    private fun findUserNameAndEmail(userEmail: String): Mono<MixetteUserDonationDto> =
        // We try to find if user is known
        userService.findOneByEncryptedEmail(cryptographer.encrypt(userEmail)!!)
            .map { it.toUser() }
            .switchIfEmpty { Mono.just(User("")) }
            .flatMap { user ->
                // Now we search if we have a ticket
                ticketService.findByEmail(userEmail)
                    .map { it.toEntity() }
                    .switchIfEmpty(Mono.just(FinalTicket("", "", lastname = "", firstname = "")))
                    .map { ticket ->
                        // ticket and user can be null but not the two at the same time
                        MixetteUserDonationDto(
                            name = if (!user?.login.isNullOrEmpty()) "${user.firstname} ${user.lastname}"
                                else ticket?.let { "${ticket.firstname} ${ticket.lastname}" } ?: "",
                            email = cryptographer.encrypt(userEmail)!!,
                            ticketNumber = ticket?.number,
                            login = user?.login
                        )
                    }
            }

    fun adminSaveDonation(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            val organizationLogin: String = formData["organizationLogin"]!!
            val userEmail: String = formData["userEmail"]!!
            userService.findOne(organizationLogin).flatMap { receiver ->
                findUserNameAndEmail(userEmail).flatMap { donor ->
                    val errors = mutableMapOf<String, String>()

                    if (donor.login.isNullOrEmpty() && donor.ticketNumber.isNullOrEmpty()) {
                        errors["userLogin"] = "admin.donations.error.userLogin.required"
                    }
                    if (receiver == null) {
                        errors["organizationLogin"] = "admin.donations.error.organizationLogin.required"
                    }
                    val quantity = formData["quantity"]?.toNumber()
                    if (quantity == null || quantity <= 0) {
                        errors["quantity"] = "admin.donations.error.quantity.invalid"
                    }
                    repository
                        .findOne(formData["id"] ?: "")
                        .map { it }
                        .switchIfEmpty { Mono.just(MixetteDonation(CURRENT_EVENT)) }
                        .flatMap {
                            persistDonation(
                                donation = it,
                                donor = donor,
                                receiver = receiver.toUser(),
                                quantity = quantity,
                                errors = errors
                            )
                        }
                }
            }
        }

    fun adminDeleteDonation(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            repository
                .deleteOne(formData["id"]!!)
                .then(seeOther("${properties.baseUri}$LIST_URI"))
        }
}
