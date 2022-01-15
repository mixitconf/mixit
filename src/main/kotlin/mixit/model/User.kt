package mixit.model

import mixit.util.Cryptographer
import mixit.util.encodeToBase64
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

object Users {
    const val DEFAULT_IMG_URL = "/images/png/mxt-icon--default-avatar.png"
}

@Document
data class User(
    @Id val login: String,
    @TextIndexed(weight = 10F) val firstname: String,
    @TextIndexed(weight = 10F) val lastname: String,
    val email: String?,
    val company: String? = null,
    @TextIndexed(weight = 5F) val description: Map<Language, String> = emptyMap(),
    val emailHash: String? = null,
    val photoUrl: String? = Users.DEFAULT_IMG_URL,
    val role: Role = Role.USER,
    val links: List<Link> = emptyList(),
    val legacyId: Long? = null,
    var tokenExpiration: LocalDateTime = LocalDateTime.now().minusDays(1),
    var token: String = "empty-token",
    var externalAppToken: String? = null
) {
    companion object {
        fun miXiTUser(): User = User("mixit", "", "MiXiT", "")
    }

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
    token = UUID.randomUUID().toString().substring(0, 14).replace("-", ""),
    externalAppToken = if (generateExternalToken) UUID.randomUUID().toString().substring(0, 14)
        .replace("-", "") else externalAppToken
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

fun User.anonymize() = this.copy(tokenExpiration = LocalDateTime.now(), token = "")
