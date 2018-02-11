package mixit.util.validator

import mixit.web.StringEscapers
import org.springframework.stereotype.Component

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 11/02/18.
 */
@Component
class MarkdownValidator {

    val escaper = StringEscapers().MARKDOWN

    fun isValid(value: String?): Boolean {
        if (value == null || value.length == 0) {
            return true
        }

        return value.equals(escaper.escape(value))
    }
}
