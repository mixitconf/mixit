package mixit.talk.spi.sessionize.dto

import mixit.MixitApplication
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.util.camelCase
import mixit.util.stripAccents

data class SessionizeSpeaker(
    val id: String,
    val firstName: String,
    val lastName: String,
    val bio: String,
    val tagLine: String?,
    val profilePicture: String,
    val links: List<SessionizeLink>,
    val email: String?,
) {
    val pictureExtension: String by lazy {
        profilePicture.substringAfterLast(".")
    }

    val picture: String by lazy {
        "/images/speakers/$CURRENT_EVENT/${
            firstName.camelCase().stripAccents().replace(" ", "_")
        }_${lastName.camelCase().stripAccents().replace(" ", "_")}.$pictureExtension"
    }
}
