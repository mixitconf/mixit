package mixit.user.handler.dto

import mixit.talk.model.Language
import mixit.user.handler.logoType
import mixit.user.handler.logoWebpUrl
import mixit.user.model.PhotoShape
import mixit.user.model.Role
import mixit.user.model.User
import mixit.util.markFoundOccurrences
import mixit.util.toHTML
import mixit.util.toUrlPath

class UserDto(
    val login: String,
    val firstname: String,
    val lastname: String,
    var email: String? = null,
    var company: String? = null,
    var description: String,
    var emailHash: String? = null,
    var photoUrl: String? = null,
    val photoShape: PhotoShape? = null,
    val role: Role,
    var links: List<LinkDto>,
    val logoType: String?,
    val logoWebpUrl: String? = null,
    val isAbsoluteLogo: Boolean = photoUrl?.startsWith("http") ?: false,
    val path: String = login.toUrlPath(),
    val cfpId: String? = null,
    val newsletterSubscriber: Boolean = false
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
        photoShape ?: PhotoShape.Square,
        role,
        links.mapIndexed { index, link -> link.toLinkDto(index) },
        logoType(photoUrl),
        logoWebpUrl(photoUrl),
        cfpId = cfpId,
        newsletterSubscriber = newsletterSubscriber
    )
