package mixit.mixette.handler

import mixit.mixette.model.MixetteDonation
import mixit.user.model.CachedOrganization

interface MixetteDonationDto{
    fun populate(number: Int, quantity: Int, amount: Double): MixetteDonationDto

    val number: Int
    val quantity: Int
    val amount: Double
}

data class MixetteUserDonationDto(
    val name: String,
    val email: String,
    val ticketNumber: String? = null,
    val login: String? = null,
    override val number: Int = 0,
    override val quantity: Int = 0,
    override val amount: Double = 0.0
): MixetteDonationDto {
    override fun populate(number: Int, quantity: Int, amount: Double) =
        this.copy(name = this.name, number = number, quantity = quantity, amount = amount)
}

data class MixetteOrganizationDonationDto(
    val name: String,
    val login: String,
    override val number: Int = 0,
    override val quantity: Int = 0,
    override val amount: Double = 0.0
): MixetteDonationDto{
    override fun populate(number: Int, quantity: Int, amount: Double) =
        this.copy(name = this.name, number = number, quantity = quantity, amount = amount)
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