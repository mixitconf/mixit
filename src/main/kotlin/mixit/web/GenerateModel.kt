package mixit.web

import com.samskivert.mustache.Mustache
import mixit.MixitProperties
import mixit.model.Role
import mixit.util.MarkdownConverter
import mixit.util.localePrefix
import org.springframework.context.MessageSource
import org.springframework.web.server.WebSession
import org.springframework.web.util.UriUtils
import java.util.*

fun generateModel(properties: MixitProperties,
                                path: String,
                                locale: Locale,
                                session: WebSession,
                                messageSource: MessageSource,
                                markdownConverter: MarkdownConverter
                  ) = mutableMapOf<String, Any>().apply {

        val email = session.getAttribute<String>("email")
        val role = session.getAttribute<Role>("role")
        email?.let {
            this["email"] = it
            if((role !=null && role == Role.STAFF)) this["admin"] = true
            this["connected"] = true
        }
        this["locale"] = locale.toString()
        this["localePrefix"] = localePrefix(locale)
        this["en"] = locale.language == "en"
        this["fr"] = locale.language == "fr"
        this["switchLangUrl"] = if (locale.language == "en") "/fr" + path else "/en" + path
        this["baseUri"] = properties.baseUri!!
        this["uri"] = "${properties.baseUri!!}$path"
        this["i18n"] = Mustache.Lambda { frag, out ->
            val tokens = frag.execute().split("|")
            out.write(messageSource.getMessage(tokens[0], tokens.slice(IntRange(1, tokens.size - 1)).toTypedArray(), locale))
        }
        this["urlEncode"] = Mustache.Lambda { frag, out -> out.write(UriUtils.encodePathSegment(frag.execute(), "UTF-8")) }
        this["markdown"] = Mustache.Lambda { frag, out -> out.write(markdownConverter.toHTML(frag.execute())) }
}.toMap()

fun generateModelForExernalCall(baseUri: String,
                                locale: Locale,
                                messageSource: MessageSource
) = mutableMapOf<String, Any>().apply {

    this["locale"] = locale.toString()
    this["localePrefix"] = localePrefix(locale)
    this["en"] = locale.language == "en"
    this["fr"] = locale.language == "fr"
    this["baseUri"] = baseUri
    this["i18n"] = Mustache.Lambda { frag, out ->
        val tokens = frag.execute().split("|")
        out.write(messageSource.getMessage(tokens[0], tokens.slice(IntRange(1, tokens.size - 1)).toTypedArray(), locale))
    }
    this["urlEncode"] = Mustache.Lambda { frag, out -> out.write(UriUtils.encodePathSegment(frag.execute(), "UTF-8")) }
}