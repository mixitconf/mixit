package mixit.talk.spi.sessionize.dto

import mixit.MixitApplication
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.util.camelCase
import mixit.util.stripAccents

data class SessionizeSpeaker(
    val id: String,
    val firstName: String,
    val lastName: String,
    val bio: String?,
    val tagLine: String?,
    val profilePicture: String,
    val links: List<SessionizeLink>,
    val email: String?,
) {
    val pictureExtension: String by lazy {
        profilePicture.substringAfterLast(".").replace("jpeg", "jpg")
    }

    val picture: String by lazy {
        "${firstName} ${lastName}"
            .stripAccents()
            .replace(" ", "_")
            .replace("__", "_")
            .let { "/images/speakers/$CURRENT_EVENT/$it.$pictureExtension" }
    }
}
