package mixit.util.validator

import org.springframework.stereotype.Component
import java.net.MalformedURLException
import java.net.URL

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 11/02/18.
 */
@Component
class UrlValidator {

    fun isValid(value: String?): Boolean {
        if (value == null || value.isEmpty()) {
            return true
        }

        val url: URL
        try {
            url = URL(value)
        } catch (e: MalformedURLException) {
            return false
        }

        if ("http" != url.protocol && "https" != url.protocol) {
            return false
        }

        return url.port == 80 || url.port == -1
    }
}
