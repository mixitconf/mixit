package mixit.util.validator

import java.net.IDN
import java.util.regex.Pattern
import mixit.util.EmailValidatorException
import org.springframework.stereotype.Component

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 11/02/18.
 */
@Component
class EmailValidator {

    fun check(value: String?): String = if (isValid(value)) value!!.trim().lowercase() else throw EmailValidatorException()

    fun isValid(value: String?): Boolean {
        if (value == null || value.length == 0) {
            return false
        }

        // split email at '@' and consider local and domain part separately;
        // note a split limit of 3 is used as it causes all characters following to an (illegal) second @ character to
        // be put into a separate array element, avoiding the regex application in this case since the resulting array
        // has more than 2 elements
        val emailParts = value.toString().split("@".toRegex(), 3).toTypedArray()
        if (emailParts.size != 2) {
            return false
        }

        return if (!matchLocalPart(emailParts[0])) false else matchDomain(emailParts[1])
    }

    private fun matchLocalPart(localPart: String): Boolean {
        if (localPart.length > MAX_LOCAL_PART_LENGTH) {
            return false
        }
        val matcher = LOCAL_PART_PATTERN.matcher(localPart)
        return matcher.matches()
    }

    private fun matchDomain(domain: String): Boolean {
        // if we have a trailing dot the domain part we have an invalid email address.
        // the regular expression match would take care of this, but IDN.toASCII drops the trailing '.'
        if (!domain.contains(".") || domain.endsWith(".") || domain.startsWith(".")) {
            return false
        }

        val asciiString: String
        try {
            asciiString = IDN.toASCII(domain)
        } catch (e: IllegalArgumentException) {
            return false
        }

        if (asciiString.length > MAX_DOMAIN_PART_LENGTH) {
            return false
        }

        val matcher = DOMAIN_PATTERN.matcher(asciiString)
        return matcher.matches()
    }

    companion object {
        private val LOCAL_PART_ATOM = "[a-z0-9!#$%&'*+/=?^_`{|}~\u0080-\uFFFF-]"
        private val DOMAIN_LABEL = "[a-z0-9!#$%&'*+/=?^_`{|}~-]"
        private val DOMAIN = "$DOMAIN_LABEL+(\\.$DOMAIN_LABEL+)*"
        private val IP_DOMAIN = "\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\]"
        private val MAX_LOCAL_PART_LENGTH = 64
        /**
         * This is the maximum length of a domain name. But be aware that each label (parts separated by a dot) of the
         * domain name must be at most 63 characters long. This is verified by [IDN.toASCII].
         */
        private val MAX_DOMAIN_PART_LENGTH = 255

        /**
         * Regular expression for the local part of an email address (everything before '@')
         */
        private val LOCAL_PART_PATTERN = Pattern.compile("$LOCAL_PART_ATOM+(\\.$LOCAL_PART_ATOM+)*", Pattern.CASE_INSENSITIVE)

        /**
         * Regular expression for the domain part of an email address (everything after '@')
         */
        private val DOMAIN_PATTERN = Pattern.compile("$DOMAIN|$IP_DOMAIN", Pattern.CASE_INSENSITIVE)
    }
}
