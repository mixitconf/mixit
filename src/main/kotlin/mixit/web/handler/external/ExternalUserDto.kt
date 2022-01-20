package mixit.web.handler.external

import mixit.model.Language
import mixit.model.Link
import mixit.model.User
import mixit.model.Users

data class ExternalUserDto(
    val login: String,
    val firstname: String,
    val lastname: String,
    val links: List<Link> = listOf(),
    val description: Map<Language, String> = emptyMap(),
    val photo: String? = null,
    val company: String? = null
) {
    constructor(user: User) : this(
        user.login,
        user.firstname,
        user.lastname,
        user.links,
        user.description,
        user.photoUrl ?: Users.DEFAULT_IMG_URL,
        user.company
    )
}