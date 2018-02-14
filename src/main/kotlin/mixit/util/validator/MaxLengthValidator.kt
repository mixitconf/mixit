package mixit.util.validator

import org.springframework.stereotype.Component

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 11/02/18.
 */
@Component
class MaxLengthValidator {

    fun isValid(value: String, max: Int): Boolean {
        val length = value.length
        return length <= max
    }
}
