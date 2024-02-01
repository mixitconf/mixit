package mixit.talk.spi.sessionize.dto


data class SessionizeSession(
    val id: String,
    val title: String,
    val description: String,
    val startsAt: String?,
    val endsAt: String?,
    val speakers: List<String>,
    val categoryItems: List<Long>,
    val roomId: String?
)
