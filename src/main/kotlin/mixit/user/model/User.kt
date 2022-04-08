package mixit.user.model

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
    val role: Role = Role.USER,
    val links: List<Link> = emptyList(),
    val legacyId: Long? = null,
    var tokenExpiration: LocalDateTime = LocalDateTime.now().minusDays(1),
    var token: String = "empty-token",
    var externalAppToken: String? = null,
    var newsletterSubscriber: Boolean = false
) {
    val tokenLifeTime: Duration
        get() = Duration.between(LocalDateTime.now(), tokenExpiration)
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

fun User.anonymize() = this.copy(
    tokenExpiration = LocalDateTime.parse("2018-01-01T00:00:00.00"),
    token = "",
    externalAppToken = null
)
