package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class User(
        @Id val login: String,
        val firstname: String,
        val lastname: String,
        val email: String,
        val company: String? = null,
        val shortDescription: String? = null,
        val longDescription: String? = null,
        val logoUrl: String? = null,
        val events: List<String> = emptyList(),
        val role: Role = Role.ATTENDEE,
        val links: List<Link> = emptyList()
)

enum class Role {
    STAFF,
    SPEAKER,
    SPONSOR,
    ATTENDEE
}
