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

    fun sanitize(value: String?): String {
        if (value == null || value.length == 0) {
            return ""
        }
        return value
                .replace("&", "&amp;")
                .replace("\"", "&#${'"'.toInt()};")
                .replace("'", "&#${'\''.toInt()};")
                .replace("`", "&#${'`'.toInt()};")
                .replace("@", "&#${'@'.toInt()};")
                .replace("=", "&#${'='.toInt()};")
                .replace("+", "&#${'+'.toInt()};")
                .replace(">", "&gt;")
                .replace("<", "&lt;")

    }

    fun isValid(value: String?): Boolean {
        if (value == null || value.length == 0) {
            return true
        }
        if(!value.equals(escaper.escape(value))){
            System.out.println(value)
            System.out.println(escaper.escape(value))
        }
        return value.equals(escaper.escape(value))
    }
}
