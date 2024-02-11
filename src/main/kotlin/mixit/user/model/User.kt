package mixit.user.model

import mixit.MixitApplication.Companion.MIXIT
import mixit.MixitApplication.Companion.MIXIT_EMAIL
import mixit.security.model.Cryptographer
import mixit.talk.model.Language
import mixit.util.encodeToBase64
import mixit.util.newToken
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Duration
import java.time.LocalDateTime

object Users {
    const val DEFAULT_IMG_URL = "/images/png/mxt-icon--default-avatar.png"
}

@Document
data class User(
    @Id val login: String = "mixit",
    @TextIndexed(weight = 10F) val firstname: String = "",
    @TextIndexed(weight = 10F) val lastname: String = "",
    val email: String? = null,
    val company: String? = null,
    @TextIndexed(weight = 5F) val description: Map<Language, String> = emptyMap(),
    val emailHash: String? = null,
    val photoUrl: String? = null,
    val photoShape: PhotoShape? = null,
    val role: Role = Role.USER,
    val links: List<Link> = emptyList(),
    val legacyId: Long? = null,
    var tokenExpiration: LocalDateTime = LocalDateTime.now().minusDays(1),
    var token: String = "empty-token",
    var externalAppToken: String? = null,
    val cfpId: String? = null,
    var newsletterSubscriber: Boolean = false,
) {
    companion object {
        fun empty(): User =
            User(login = "", firstname = "", lastname = "", email = "")

        fun mixit(): User =
            User(login = MIXIT, firstname = MIXIT, lastname = MIXIT, email = MIXIT_EMAIL)
    }

    fun tokenLifeTime(): Duration =
        Duration.between(LocalDateTime.now(), tokenExpiration)

    fun filterOn(criteria: String?, cryptographer: Cryptographer) =
        criteria.isNullOrBlank() ||
                login.contains(criteria, true) ||
                firstname.contains(criteria, true) ||
                lastname.contains(criteria, true) ||
                cryptographer.decrypt(email)?.contains(criteria, true) ?: false
}

enum class PhotoShape {
    Square,
    Rectangle
}

enum class Role {
    STAFF,
    STAFF_IN_PAUSE,
    USER,
    VOLUNTEER
}

fun User.generateNewToken(generateExternalToken: Boolean = false) = this.copy(
    tokenExpiration = LocalDateTime.now().plusHours(48),
    token = newToken(),
    externalAppToken = if (generateExternalToken) newToken() else externalAppToken
)

fun User.updateEmail(cryptographer: Cryptographer, newEmail: String) =
    this.copy(email = cryptographer.encrypt(newEmail))

fun User.jsonToken(cryptographer: Cryptographer) = "${cryptographer.decrypt(email)}:$token".encodeToBase64()!!

fun User.hasValidToken(token: String) = this.token == token.trim() && tokenExpiration.isAfter(LocalDateTime.now())

fun User.hasValidTokens(token: String?, externalAppToken: String?): Boolean {
    if (token != null && hasValidToken(token)) {
        return true
    }
    if (externalAppToken != null && this.externalAppToken == externalAppToken.trim()) {
        return true
    }
    return false
}

fun User.anonymize(cryptographer: Cryptographer?) =
    if (cryptographer == null) {
        this.copy(
            tokenExpiration = LocalDateTime.parse("2018-01-01T00:00:00.00"),
            token = "",
            externalAppToken = null
        )
    } else {
        this.copy(
            login = cryptographer.encrypt(login)!!,
            firstname = cryptographer.encrypt(firstname)!!,
            lastname = cryptographer.encrypt(lastname)!!,
            company = cryptographer.encrypt(company),
            emailHash = cryptographer.encrypt(emailHash),
            description = description.mapValues { cryptographer.encrypt(it.value)!! },
            photoUrl = cryptographer.encrypt(photoUrl),
            links = links.map {
                it.copy(
                    name = cryptographer.encrypt(it.name)!!,
                    url = cryptographer.encrypt(it.url)!!
                )
            },
            tokenExpiration = LocalDateTime.parse("2018-01-01T00:00:00.00"),
            token = "",
            externalAppToken = null
        )
    }

fun User.desanonymize(cryptographer: Cryptographer) =
    this.copy(
        login = cryptographer.decrypt(login)!!,
        firstname = cryptographer.decrypt(firstname)!!,
        lastname = cryptographer.decrypt(lastname)!!,
        company = cryptographer.decrypt(company),
        emailHash = cryptographer.decrypt(emailHash),
        description = description.mapValues { cryptographer.decrypt(it.value)!! },
        photoUrl = cryptographer.decrypt(photoUrl),
        links = links.map {
            it.copy(
                name = cryptographer.decrypt(it.name)!!,
                url = cryptographer.decrypt(it.url)!!
            )
        }
    )
