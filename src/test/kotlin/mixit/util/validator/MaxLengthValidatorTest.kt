package mixit.util.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test {@link MaxLengthValidator}
 */
class MaxLengthValidatorTest {

    val validator = MaxLengthValidator()

    @Test
    fun `should valid text`() {
        assertThat(validator.isValid("mixit", 10)).isTrue()
        assertThat(validator.isValid("", 10)).isTrue()
        assertThat(validator.isValid("mixit", 3)).isFalse()
    }
}
