package mixit.data.dto

import mixit.model.*
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
    fun toUser(events: List<String> = emptyList(), roles: Set<Role> = setOf(Role.ATTENDEE)): User {
        return User(
                login ?: "user$idMember",
                firstname ?: "",
                lastname ?: "",
                email ?: "",
                company ?: "",
                shortDescription ?: "",
                longDescription ?: "",
                logo ?: "",
                events,
                roles,
                links = userLinks.map { link -> Link(link.key, link.value) }
        )
    }
}
