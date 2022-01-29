package mixit.user.handler

import mixit.talk.model.Language
import mixit.user.model.Link
import mixit.user.model.Role
import mixit.user.model.User
import mixit.util.camelCase
import mixit.util.markFoundOccurrences
import mixit.util.toHTML
import mixit.util.toUrlPath

class SpeakerStarDto(
    val login: String,
    val key: String,
    val name: String
)

fun User.toSpeakerStarDto() = SpeakerStarDto(login, lastname.lowercase().replace("è", "e"), "${firstname.camelCase()} ${lastname.camelCase()}")

class UserDto(
    val login: String,
    val firstname: String,
    val lastname: String,
    var email: String? = null,
    var company: String? = null,
    var description: String,
    var emailHash: String? = null,
    var photoUrl: String? = null,
    val role: Role,
    var links: List<Link>,
    val logoType: String?,
    val logoWebpUrl: String? = null,
    val isAbsoluteLogo: Boolean = photoUrl?.startsWith("http") ?: false,
    val path: String = login.toUrlPath()
)

fun User.toDto(language: Language, searchTerms: List<String> = emptyList()) =
    UserDto(
        login,
        firstname.markFoundOccurrences(searchTerms),
        lastname.markFoundOccurrences(searchTerms),
        email,
        company,
        description[language]?.toHTML()?.markFoundOccurrences(searchTerms) ?: "",
        emailHash,
        photoUrl,
        role,
        links,
        logoType(photoUrl),
        logoWebpUrl(photoUrl)
    )
