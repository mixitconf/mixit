package mixit.favorite.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Favorite(
    val email: String,
    val talkId: String,
    @Id val id: String? = null
)
