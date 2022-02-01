package mixit.mixette.handler

import mixit.MixitProperties
import mixit.event.handler.AdminEventHandler.Companion.CURRENT_EVENT
import mixit.event.model.EventService
import mixit.mixette.model.MixetteDonation
import mixit.mixette.repository.MixetteDonationRepository
import mixit.user.cache.CachedOrganization
import mixit.user.model.User
import mixit.user.repository.UserRepository
import mixit.util.extractFormData
import mixit.util.seeOther
import mixit.util.toNumber
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AdminMixetteHandler(
    private val repository: MixetteDonationRepository,
    private val userRepository: UserRepository,
    private val service: EventService,
    private val properties: MixitProperties
) {

    companion object {
        const val TEMPLATE_BY_USER_LIST = "admin-mixette-donors"
        const val TEMPLATE_BY_ORGA_LIST = "admin-mixette-organizations"
        const val TEMPLATE_BY_USER = "admin-mixette-donor"
        const val TEMPLATE_BY_ORGA = "admin-mixette-organization"
        const val TEMPLATE_EDIT = "admin-mixette-donation"
        const val LIST_URI = "/admin/mixette-organization"
    }

    fun adminDonorDonations(req: ServerRequest): Mono<ServerResponse> =
        adminGroupDonations(TEMPLATE_BY_USER_LIST) { UserDonationDto(it.userLogin, it.username) }

    fun adminOrganizationDonations(req: ServerRequest): Mono<ServerResponse> =
        adminGroupDonations(TEMPLATE_BY_ORGA_LIST) { UserDonationDto(it.organizationLogin, it.organizationName) }

    private fun adminGroupDonations(tartget: String, transformation: (MixetteDonation) -> UserDonationDto): Mono<ServerResponse> {
        val donationByOrgas = repository.findAllByYear(CURRENT_EVENT).collectList().map { donations ->
            donations
                .groupBy { transformation.invoke(it) }
                .map { entry ->
                    entry.key.copy(
                        number = entry.value.size,
                        quantity = entry.value.sumOf { it.quantity },
                        amount = entry.value.sumOf { it.quantity * properties.mixetteValue.toDouble() }
                    )
                }
        }
        return ok().render(
            tartget,
            mapOf(Pair("donations", donationByOrgas), Pair("title", "admin.donations.title"))
        )
    }

    fun addDonation(req: ServerRequest): Mono<ServerResponse> =
        this.adminDonation()

    fun editDonation(req: ServerRequest): Mono<ServerResponse> =
        repository.findOne(req.pathVariable("id")).flatMap { this.adminDonation(it) }

    fun editOrga(req: ServerRequest): Mono<ServerResponse> =
        req.pathVariable("login").let { login ->
            this.adminGroupDonation(
                login,
                TEMPLATE_BY_ORGA,
                repository.findByOrganizationLogin(login, CURRENT_EVENT).collectList()
            ) { UserDonationDto(it.organizationLogin, it.organizationName) }
        }


    fun editDonor(req: ServerRequest): Mono<ServerResponse> =
        req.pathVariable("login").let { login ->
            this.adminGroupDonation(
                login,
                TEMPLATE_BY_USER,
                repository.findByLogin(login, CURRENT_EVENT).collectList()
            ) { UserDonationDto(it.userLogin, it.username) }
        }

    private fun adminGroupDonation(
        login: String,
        target: String,
        monoDonations: Mono<List<MixetteDonation>>,
        transformation: (MixetteDonation) -> UserDonationDto
    ): Mono<ServerResponse> =
        userRepository.findOne(login).flatMap { user ->
            monoDonations.flatMap { donations ->
                val quantity = donations.sumOf { it.quantity }
                val userDonation = transformation.invoke(donations.first()).copy(
                    number = donations.size,
                    quantity = quantity,
                    amount = quantity * properties.mixetteValue.toDouble()
                )
                ok().render(
                    target,
                    mapOf(
                        Pair("donations", donations),
                        Pair("userDonation", userDonation),
                        Pair("user", user),
                        Pair("title", "admin.donations.title")
                    )
                )
            }
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
                        Pair("donation", donation),
                        Pair("organizations", event.organizations.map { OrganizationDto(it, donation) }),
                        Pair("errors", errors),
                        Pair("hasErrors", errors.isNotEmpty())
                    )
                )
            }

    private fun persistDonation(
        donation: MixetteDonation,
        donor: User?,
        receiver: User?,
        quantity: Int?,
        errors: Map<String, String>
    ): Mono<ServerResponse> {
        if (errors.isNotEmpty()) {
            return adminDonation(donation, errors)
        }
        val newDonation = donation.copy(
            userLogin = donor?.login ?: "",
            username = "${donor?.firstname} ${donor?.lastname}",
            organizationLogin = receiver?.login ?: "",
            organizationName = receiver?.company ?: "${receiver?.firstname} ${receiver?.lastname}",
            quantity = quantity!!
        )
        if (newDonation.id == null) {
            return repository.insert(newDonation).flatMap { seeOther("${properties.baseUri}$LIST_URI") }
        }
        return repository.update(newDonation).flatMap { seeOther("${properties.baseUri}$LIST_URI") }
    }

    fun adminSaveDonation(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            val userLogin: String = formData["userLogin"]!!
            val organizationLogin: String = formData["organizationLogin"]!!
            userRepository
                .findAllByIds(listOf(userLogin, organizationLogin))
                .collectList()
                .flatMap { users ->
                    val errors = mutableMapOf<String, String>()
                    val donor = users.firstOrNull { it.login == userLogin }
                    if (donor == null) {
                        errors["userLogin"] = "admin.donations.error.userLogin.required"
                    }
                    val receiver = users.firstOrNull { it.login == organizationLogin }
                    if (receiver == null) {
                        errors["organizationLogin"] = "admin.donations.error.organizationLogin.required"
                    }
                    val quantity = formData["quantity"]?.toNumber()
                    if (quantity == null || quantity <= 0) {
                        errors["quantity"] = "admin.donations.error.quantity.invalid"
                    }
                    repository
                        .findOne(formData["id"] ?: "")
                        .flatMap {
                            persistDonation(it, donor, receiver, quantity, errors)
                        }
                        .switchIfEmpty(
                            persistDonation(
                                MixetteDonation(CURRENT_EVENT),
                                donor,
                                receiver,
                                quantity,
                                errors
                            )
                        )
                }
        }

    fun adminDeleteDonation(req: ServerRequest): Mono<ServerResponse> =
        req.extractFormData().flatMap { formData ->
            repository
                .deleteOne(formData["id"]!!)
                .then(seeOther("${properties.baseUri}$LIST_URI"))
        }

    data class UserDonationDto(
        val login: String,
        val name: String,
        val number: Int = 0,
        val quantity: Int = 0,
        val amount: Double = 0.0
    )

    data class OrganizationDto(
        val login: String,
        val name: String,
        val photoUrl: String? = null,
        val selected: Boolean = false
    ) {
        constructor(user: CachedOrganization, donation: MixetteDonation) : this(
            user.login,
            user.company,
            user.photoUrl,
            selected = (user.login == donation.organizationLogin)
        )
    }
}
