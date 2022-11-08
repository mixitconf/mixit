package mixit.util.validator

import java.net.MalformedURLException
import java.net.URL

object UrlValidator {

    fun isValid(value: String?): Boolean {
        if (value.isNullOrEmpty()) {
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
