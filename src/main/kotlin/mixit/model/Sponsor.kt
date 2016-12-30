package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Sponsor(
        var name: String,
        var login: String,
        var email: String,
        var shortDescription: String,
        var longDescription: String,
        var logoUrl: String,
        var links: List<Link>,
        @Id var id: String? = null
)
