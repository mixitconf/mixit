package mixit.mixette.handler

import mixit.mixette.model.MixetteDonation
import mixit.security.model.Cryptographer
import mixit.user.model.CachedOrganization
import java.time.Instant

interface MixetteDonationDto {
    fun populate(number: Int, quantity: Int, amount: Double): MixetteDonationDto
    fun updateRank(number: Int): MixetteDonationDto

    val number: Int
    val quantity: Int
    val amount: Double
    val rank: Int
}

data class MixetteUserDonationDto(
    val name: String,
    val email: String,
    val ticketNumber: String? = null,
    val login: String? = null,
    val id: String? = null,
    override val rank: Int = 0,
    override val number: Int = 0,
    override val quantity: Int = 0,
    override val amount: Double = 0.0
) : MixetteDonationDto {
    override fun populate(number: Int, quantity: Int, amount: Double) =
        this.copy(name = this.name, number = number, quantity = quantity, amount = amount)

    override fun updateRank(number: Int): MixetteDonationDto =
        this.copy(rank = number)

}

data class MixetteOrganizationDonationDto(
    val name: String,
    val login: String,
    override val rank: Int = 0,
    override val number: Int = 0,
    override val quantity: Int = 0,
    override val amount: Double = 0.0
) : MixetteDonationDto {
    override fun populate(number: Int, quantity: Int, amount: Double) =
        this.copy(name = this.name, number = number, quantity = quantity, amount = amount)

    override fun updateRank(number: Int): MixetteDonationDto =
        this.copy(rank = number)
}

data class MixetteOrganizationDto(
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

data class MixetteDonationDetailedDto(
    val year: String,
    val ticketNumber: String?,
    val userLogin: String?,
    val userEmail: String,
    val username: String,
    val organizationLogin: String,
    val organizationName: String,
    val quantity: Int,
    val createdBy: String?,
    val updatedBy: String?,
    val addedAt: Instant,
    val id: String?
) {
    constructor(
        donation: MixetteDonation,
        cryptographer: Cryptographer,
        username: String,
        organizationName: String
    ) : this(
        year = donation.year,
        ticketNumber = cryptographer.decrypt(donation.encryptedTicketNumber),
        userLogin = donation.userLogin,
        userEmail = cryptographer.decrypt(donation.encryptedUserEmail)!!,
        organizationLogin = donation.organizationLogin,
        quantity = donation.quantity,
        createdBy = donation.createdBy,
        updatedBy = donation.updatedBy,
        addedAt = donation.addedAt,
        username = username,
        organizationName = organizationName,
        id = donation.id
    )
}
