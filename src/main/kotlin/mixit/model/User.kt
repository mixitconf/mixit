package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class User(
        @Id val login: String,
        val firstname: String,
        val lastname: String,
        var email: String,
        var company: String? = null,
        var shortDescription: String = "",
        var longDescription: String = "",
        var logoUrl: String? = null,
        val events: List<String> = emptyList(),
        val role: Role = Role.ATTENDEE,
        var links: List<Link> = emptyList(),
        val legacyId: Long? = null
)

enum class Role {
    STAFF,
    SPEAKER,
    SPONSOR,
    ATTENDEE
}

class Logo(){
    companion object {

        fun logoWebPUrl(url:String): String? {
            if(url.endsWith("png") || url.endsWith("jpg")){
                return url.replace("png", "webp").replace("jpg", "webp")
            }
            return null
        }

        fun logoType(url:String): String {
            if(url.endsWith("svg")){
                return "image/svg+xml"
            }
            if(url.endsWith("png")){
                return "image/png"
            }
            if(url.endsWith("jpg")){
                return "image/jpeg"
            }
            throw IllegalArgumentException("Extension not supported")
        }
    }
}