package mixit.util.validator

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 11/02/18.
 */
class MaxLengthValidator {

    fun isValid(value: String, max: Int): Boolean {
        val length = value.length
        return length <= max
    }
}
