package mixit.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.user.model.Link

object AdminUtils {

    fun Any.toJson(objectMapper: ObjectMapper): String =
        objectMapper.writeValueAsString(this).replace("\"", "&quot;")

    fun String.toLinks(objectMapper: ObjectMapper): List<Link> =
        if (this.isEmpty()) emptyList() else objectMapper.readValue(this)

    fun String.toLink(objectMapper: ObjectMapper): Link? =
        if (this.isEmpty()) null else objectMapper.readValue(this)
}