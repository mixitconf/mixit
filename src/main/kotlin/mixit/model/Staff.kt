package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Staff(
        @Id var id: String,
        var firstname: String,
        var lastname: String,
        var login: String,
        var email: String,
        var shortDescription: String,
        var longDescription: String,
        var links: List<Link> = emptyList()
)
