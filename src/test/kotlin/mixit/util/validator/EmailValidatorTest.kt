package mixit.util.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test {@link EmailValidator}
 */
class EmailValidatorTest {

    val validator = EmailValidator()

    @Test
    fun `should accept correct email`() {
        assertThat(validator.isValid("contact@mix-it.fr")).isTrue()
        assertThat(validator.isValid("guillaume@dev-mind.fr")).isTrue()
    }

    @Test
    fun `should not accept malformed email`() {
        assertThat(validator.isValid("contactmix-it.fr")).isFalse()
        assertThat(validator.isValid("contact@mix-it")).isFalse()
        assertThat(validator.isValid("contact@mix-it.")).isFalse()
        assertThat(validator.isValid("contact@.mix-it")).isFalse()
        assertThat(validator.isValid("contact@mix.it")).isTrue()
    }

    @Test
    fun `should not accept toolong email`() {
        assertThat(validator.isValid("contacttoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolong@mix-it.fr")).isFalse()
        assertThat(validator.isValid("contact@mix-ittoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolongtoolong.fr")).isFalse()
    }
}
