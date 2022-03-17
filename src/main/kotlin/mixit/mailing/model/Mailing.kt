package mixit.mailing.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

enum class RecipientType {
    User,
    Sponsor,
    Volunteers,
    Staff,
    StaffInPause,
    Organization
}

@Document
data class Mailing(
    val title: String = "",
    val content: String = "",
    val addedAt: LocalDateTime = LocalDateTime.now(),
    val type: RecipientType? = null,
    val recipientLogins: List<String> = emptyList(),
    @Id val id: String? = null,
)
