package mixit.model

import mixit.util.MarkdownConverter
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document
data class User(
        @Id val login: String,
        val firstname: String,
        val lastname: String,
        val email: String,
        val company: String? = null,
        val description: Map<Language, String> = emptyMap(),
        val logoUrl: String? = null,
        val events: List<String> = emptyList(),
        val role: Role = Role.ATTENDEE,
        val links: List<Link> = emptyList(),
        val legacyId: Long? = null
)

enum class Role {
    STAFF,
    SPEAKER,
    SPONSOR,
    ATTENDEE
}

class UserDto(
    val login: String,
    val firstname: String,
    val lastname: String,
    var email: String,
    var company: String? = null,
    var description: String,
    var logoUrl: String? = null,
    val events: List<String>,
    val role: Role,
    var links: List<Link>
)

fun User.toDto(language: Language, markdownConverter: MarkdownConverter) =
        UserDto(login, firstname, lastname, email, company, markdownConverter.toHTML(description[language] ?: ""),
        logoUrl, events, role, links)
