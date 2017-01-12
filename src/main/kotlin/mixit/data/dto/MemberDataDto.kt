package mixit.data.dto

import mixit.model.*
import java.time.LocalDate
import java.util.*

data class MemberDataDto(
        val idMember: Long,
        var login: String?,
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
    fun toUser(events: List<String> = emptyList(), role: Role = Role.ATTENDEE): User {
        return User(
                login ?: "user$idMember",
                firstname ?: "",
                lastname ?: "",
                email ?: "",
                company ?: "",
                shortDescription ?: "",
                longDescription ?: "",
                logo?.replace("sponsors/", "/images/sponsor/") ?: "",
                events,
                role,
                links = userLinks.map { link -> Link(link.key, link.value) }
        )
    }

    fun toEventSponsoring(sponsor: User): Iterable<EventSponsoring> {
        // A sponsor has one or more sponsorshipLevel but not a classical member
        val sponsorshipLevel = level ?: listOf(LevelDataDto(SponsorshipLevel.NONE, LocalDate.now()))

        return sponsorshipLevel.map { sl ->
            EventSponsoring(
                    sl.key,
                    sponsor,
                    sl.value ?: LocalDate.now()
            )
        }
    }
}
