package mixit.util.validator

import mixit.web.StringEscapers
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 */
@Component
class MarkdownValidator {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val escaper = StringEscapers().MARKDOWN

    fun sanitize(value: String?): String {
        if (value == null || value.isEmpty()) {
            return ""
        }
        return value
            .replace("&", "&amp;")
            .replace("\"", "&#${'"'.code};")
            .replace("'", "&#${'\''.code};")
            .replace("`", "&#${'`'.code};")
            .replace("@", "&#${'@'.code};")
            .replace("=", "&#${'='.code};")
            .replace("+", "&#${'+'.code};")
            .replace(">", "&gt;")
            .replace("<", "&lt;")
    }

    fun isValid(value: String?): Boolean {
        if (value == null || value.isEmpty()) {
            return true
        }
        if (value != escaper.escape(value)) {
            logger.info("$value -> ${escaper.escape(value)}")
        }
        return value == escaper.escape(value)
    }
}
