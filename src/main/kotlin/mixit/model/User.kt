package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.*


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
) {
    companion object {
        fun encodeEmail(email: String?): String? = if (email == null) null else Base64.getEncoder().encodeToString(email.toByteArray())
        fun decodeEmail(email: String?): String? = if (email == null) null else String(Base64.getDecoder().decode(email.toByteArray()))
    }


}

enum class Role {
    STAFF,
    STAFF_IN_PAUSE,
    USER
}