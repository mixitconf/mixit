package mixit.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 20/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LinkDataDto(
        val key: String,
        val value: String
)
