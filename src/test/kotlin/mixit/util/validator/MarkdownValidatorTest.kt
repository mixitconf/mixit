package mixit.util.validator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


/**
 * Test {@link MarkdownValidator}
 */
class MarkdownValidatorTest{
    val validator = MarkdownValidator()

    @Test
    fun `should accept correct markdown`() {
        val markdown = "Développeur agile passionné par la technique et l'agilité, fondateur de [Dev-Mind](https://dev-mind.fr/)." +
                "#Vous pouvez voir mon logo" +
                "![Dev-Mind](https://www.dev-mind.fr/img/logo/logo_1500.png)"

        assertThat(validator.isValid(validator.sanitize(markdown))).isTrue()
    }

    @Test
    fun `should not accept html in markdown`() {
        val markdown = "Développeur <script>js</script> <i>passionné</i> par "

        assertThat(validator.isValid(markdown)).isFalse()
        // But accept if text is sanitized
        assertThat(validator.isValid(validator.sanitize(markdown))).isTrue()
    }

    @Test
    fun `should not accept script in markdown`() {
        val markdown = "Développeur <script src=\"monserveur.com\">js</script> <i>passionné</i> par "

        assertThat(validator.isValid(markdown)).isFalse()
        // But accept if text is sanitized
        assertThat(validator.isValid(validator.sanitize(markdown))).isTrue()
    }
}