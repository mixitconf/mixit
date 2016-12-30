package mixit.data.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 20/12/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LevelDataDto(
        val key: String?,
        val value: Instant?
)
