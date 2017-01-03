package mixit.data.dto

import mixit.model.EventSponsoring
import mixit.model.Link
import mixit.model.Sponsor
import mixit.model.SponsorshipLevel
import java.time.LocalDate
import java.util.*

data class MemberDataDto(
        val idMember: Long,
        var login: String,
        var firstname: String?,
        var lastname: String?,
        var email: String?,
        var company: String?,
        var logo: String?,
        var hash: String?,
        var sessionType: String?,
        var shortDescription: String?,
        var longDescription: String?,
        var userLinks: List<LinkDataDto> = ArrayList<LinkDataDto>(),
        var interests: List<String>?,
        var sessions: List<Long>?,
        var level: List<LevelDataDto>?
) {
    fun toSponsor(): Sponsor {
        return Sponsor(
                // Company is always defined for a sponsor
                company ?: "",
                login,
                email ?: "",
                shortDescription ?: "",
                longDescription ?: "",
                // Logo is always defined for a sponsor
                logo ?: "",
                userLinks.map { link -> Link(link.key, link.value) },
                idMember.toString()
        )
    }

    fun toEventSponsoring(year: Int): Iterable<EventSponsoring>{
        // A sponsor has one or more sponsorshipLevel but not a classical memeber
        val sponsorshipLevel = level ?: listOf(LevelDataDto(SponsorshipLevel.NONE, LocalDate.now()))

        return sponsorshipLevel.map { sl ->  EventSponsoring(
                sl.key,
                toSponsor(),
                sl.value,
                "".plus(year).plus(sl.key).plus(company)
                )}
    }
}
