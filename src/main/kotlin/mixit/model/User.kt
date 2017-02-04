package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class User(
        @Id val login: String,
        val firstname: String,
        val lastname: String,
        var email: String,
        var company: String? = null,
        var shortDescription: String? = null,
        var longDescription: String? = null,
        var logoUrl: String? = null,
        val events: List<String> = emptyList(),
        val role: Role = Role.ATTENDEE,
        var links: List<Link> = emptyList()
)

enum class Role {
    STAFF,
    SPEAKER,
    SPONSOR,
    ATTENDEE
}
