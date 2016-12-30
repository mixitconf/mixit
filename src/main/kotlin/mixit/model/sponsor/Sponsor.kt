package mixit.model.sponsor

import mixit.model.link.Link
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Sponsor(
        @Id var id: Long = 0,
        var name: String = "",
        var login: String = "",
        var email: String = "",
        var shortDescription: String = "",
        var longDescription: String = "",
        var logoUrl: String = "",
        var links: List<Link> = emptyList()
)
