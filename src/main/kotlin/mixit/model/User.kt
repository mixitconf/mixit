package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime


@Document
data class User(
        @Id val login: String,
        val firstname: String,
        val lastname: String,
        val email: String?,
        val company: String? = null,
        val description: Map<Language, String> = emptyMap(),
        val emailHash: String? = null,
        val photoUrl: String? = null,
        val role: Role = Role.USER,
        val links: List<Link> = emptyList(),
        val legacyId: Long? = null,
        var tokenExpiration: LocalDateTime = LocalDateTime.now().minusDays(1),
        var token: String = "empty-token"
)

enum class Role {
    STAFF,
    STAFF_IN_PAUSE,
    USER
}