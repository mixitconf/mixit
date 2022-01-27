package mixit.web.handler.admin

import mixit.MixitProperties
import mixit.model.MixetteDonation
import mixit.model.User
import mixit.repository.MixetteDonationRepository
import mixit.repository.UserRepository
import mixit.util.extractFormData
import mixit.util.seeOther
import mixit.util.toNumber
import mixit.web.handler.admin.AdminEventHandler.Companion.CURRENT_EVENT
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono

@Component
class AdminMixetteHandler(
    private val repository: MixetteDonationRepository,
    private val userRepository: UserRepository,
    private val properties: MixitProperties
) {

    companion object {
        const val TEMPLATE_BY_USER_LIST = "admin-mixette-donor"
        const val TEMPLATE_BY_ORGA_LIST = "admin-mixette-organization"
        const val TEMPLATE_EDIT = "admin-donation"
        const val LIST_URI = "/admin/donations"
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
        repository.findOne(req.pathVariable("donationId")).flatMap { this.adminDonation(it) }

    private fun adminDonation(
        donation: MixetteDonation = MixetteDonation(CURRENT_EVENT),
        errors: Map<String, String> = emptyMap()
    ) =
        ok().render(
            TEMPLATE_EDIT,
            mapOf(
                Pair("creationMode", donation.id == null),
                Pair("donation", donation),
                Pair("errors", errors)
            )
        )

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
        repository.save(
            donation.copy(
                userLogin = donor?.login ?: "",
                username = "${donor?.firstname} ${donor?.lastname}",
                organizationLogin = receiver?.login ?: "",
                organizationName = receiver?.company ?: "${receiver?.firstname} ${receiver?.lastname}",
                quantity = quantity!!
            )
        )
        return seeOther("${properties.baseUri}${LIST_URI}")
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
                        errors["userLogin"] = "user.form.error.userLogin.required"
                    }
                    val receiver = users.firstOrNull { it.login == organizationLogin }
                    if (receiver == null) {
                        errors["organizationLogin"] = "user.form.error.organizationLogin.required"
                    }
                    val quantity = formData["quantity"]?.toNumber()
                    repository
                        .findOne(formData["donationId"]!!)
                        .flatMap { persistDonation(it, donor, receiver, quantity, errors) }
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
}
