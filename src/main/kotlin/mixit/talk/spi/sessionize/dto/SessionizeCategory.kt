package mixit.talk.spi.sessionize.dto

data class SessionizeCategory(
    val id: Long,
    val title: String,
    val items: List<SessionizeCategoryItem>
){
    fun isFormat() = title.lowercase().contains("format")
    fun isLevel() = title.lowercase().contains("level")
    fun isLanguage() = title.lowercase().contains("language")
    fun isTrack() = !isFormat() && !isLevel() && !isLanguage()
}

data class SessionizeCategoryItem(
    val id: Long,
    val name: String,
    val sort: Int
)

