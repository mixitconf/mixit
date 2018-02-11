package mixit.util.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


/**
 * Test {@link URLValidator}
 */
class UrlValidatorTest {

    val validator = UrlValidator()

    @Test
    fun `should accept URL null or empty`() {
        assertThat(validator.isValid(null)).isTrue()
        assertThat(validator.isValid("")).isTrue()
    }

    @Test
    fun `should accept http or https adress`() {
        assertThat(validator.isValid("https://mixitconf.org/mypage")).isTrue()
        assertThat(validator.isValid("http://mixitconf.org/mypage")).isTrue()
    }

    @Test
    fun `should not accept email or ftp protocols`() {
        assertThat(validator.isValid("ftp://mixitconf.org/mypage")).isFalse()
        assertThat(validator.isValid("mailto://mixitconf.org/mypage")).isFalse()
    }

    @Test
    fun `should not accept other port than 80`() {
        assertThat(validator.isValid("https://mixitconf.org/mypage")).isTrue()
        assertThat(validator.isValid("https://mixitconf.org:80/mypage")).isTrue()
        assertThat(validator.isValid("https://mixitconf.org:8080/mypage")).isFalse()
    }
}