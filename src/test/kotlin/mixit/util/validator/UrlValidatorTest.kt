package mixit.util.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Test {@link URLValidator}
 */
class UrlValidatorTest {

    @Test
    fun `should accept URL null or empty`() {
        assertThat(UrlValidator.isValid(null)).isTrue
        assertThat(UrlValidator.isValid("")).isTrue
    }

    @Test
    fun `should accept http or https adress`() {
        assertThat(UrlValidator.isValid("https://mixitconf.org/mypage")).isTrue
        assertThat(UrlValidator.isValid("http://mixitconf.org/mypage")).isTrue
    }

    @Test
    fun `should not accept email or ftp protocols`() {
        assertThat(UrlValidator.isValid("ftp://mixitconf.org/mypage")).isFalse
        assertThat(UrlValidator.isValid("mailto://mixitconf.org/mypage")).isFalse
    }

    @Test
    fun `should not accept other port than 80`() {
        assertThat(UrlValidator.isValid("https://mixitconf.org/mypage")).isTrue
        assertThat(UrlValidator.isValid("https://mixitconf.org:80/mypage")).isTrue
        assertThat(UrlValidator.isValid("https://mixitconf.org:8080/mypage")).isFalse
    }
}
