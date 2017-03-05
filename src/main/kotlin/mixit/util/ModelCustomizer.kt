package mixit.util

import com.samskivert.mustache.Mustache
import org.springframework.context.MessageSource
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriUtils

fun customizeModel(model: MutableMap<String, Any>, exchange: ServerWebExchange, messageSource: MessageSource) {
    val locale = exchange.request.headers.acceptLanguageAsLocale
    val username = exchange.session.block().getAttribute<String>("username")
    if (username.isPresent) {
        model.put("username", username.get())
    }
    if (locale != null) {
        model.put("locale", locale.toString())
        model.put("localePrefix", if (locale.language == "en") "/en" else "")
        model.put("en", locale.language == "en")
        model.put("fr", locale.language == "fr")
        var switchLangUrl = exchange.request.uri.path
        switchLangUrl = if (locale.language == "en") switchLangUrl else "/en" + switchLangUrl
        model.put("switchLangUrl", switchLangUrl)
    }
    model.put("i18n", Mustache.Lambda { frag, out ->
        val key = frag.execute()
        out.write(messageSource.getMessage(key, null, locale))
    })
    model.put("urlEncode", Mustache.Lambda { frag, out -> out.write(UriUtils.encodePathSegment(frag.execute(), "UTF-8")) })
}
