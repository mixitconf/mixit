package mixit.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import mixit.model.link.Link
import mixit.model.sponsor.Sponsor
import java.util.*

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 20/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MemberDataDto(
        val idMember: Long,
        var login: String,
        var firstname: String?,
        var lastname: String?,
        var email: String,
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
                idMember,
                // Company is always defined for a sponsor
                company ?: "",
                login,
                email,
                shortDescription ?: "",
                longDescription ?: "",
                // Logo is always defined for a sponsor
                logo ?: "",
                userLinks
                        .filter { link -> link.value != null }
                        .map { link -> Link(link.key, link.value) })
    }
}
