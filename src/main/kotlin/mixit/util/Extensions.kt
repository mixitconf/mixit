package mixit.util

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import mixit.security.MixitWebFilter
import mixit.talk.model.Language
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.springframework.http.ResponseCookie
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.permanentRedirect
import org.springframework.web.reactive.function.server.ServerResponse.seeOther
import org.springframework.web.server.WebSession

// -------------------------
// Spring WebFlux extensions
// -------------------------

fun ServerRequest.language(): Language =
    Language.findByTag(
        this.headers().asHttpHeaders().contentLanguage?.language ?: this.headers()
            .asHttpHeaders().acceptLanguageAsLocales.first().language
    )

fun ServerRequest.locale(): Locale =
    this.headers().asHttpHeaders().contentLanguage ?: Locale.ENGLISH

suspend fun ServerRequest.extractFormData(): Map<String, String?> =
    this.body(BodyExtractors.toFormData())
        .map { data ->
            data.toSingleValueMap().mapValues { if (it.value.isNullOrEmpty()) null else it.value }
        }
        .awaitSingleOrNull() ?: emptyMap()

suspend fun ServerRequest.webSession(): WebSession =
    this.session().awaitSingle()

suspend fun ServerRequest.extractEmailFromSession(): String? =
    webSession().getAttribute<String?>(MixitWebFilter.SESSION_EMAIL_KEY)

suspend fun ServerRequest.webSessionOrNull(): WebSession? =
    this.session().awaitSingleOrNull()

suspend fun ServerRequest.currentNonEncryptedUserEmail(): String =
    this.session()
        .map { it.getAttribute(MixitWebFilter.SESSION_EMAIL_KEY) ?: "" }
        .awaitSingle()

suspend fun ServerRequest.decode(pathVariable: String): String? =
    withContext(Dispatchers.IO) {
        URLDecoder.decode(pathVariable(pathVariable), "UTF-8")
    }

suspend fun permanentRedirect(uri: String): ServerResponse = permanentRedirect(URI(uri)).build().awaitSingle()

suspend fun seeOther(uri: String): ServerResponse = seeOther(URI(uri)).build().awaitSingle()

suspend fun seeOther(uri: String, cookie: ResponseCookie): ServerResponse =
    seeOther(URI(uri)).cookie(cookie).build().awaitSingle()

suspend fun temporaryRedirect(uri: String): ServerResponse =
    ServerResponse.temporaryRedirect(URI(uri)).build().awaitSingle()

// --------------------
// Date/Time extensions
// --------------------

fun LocalDateTime.formatDate(language: Language): String =
    if (language == Language.ENGLISH) this.format(englishDateFormatter) else this.format(frenchDateFormatter)

fun LocalDateTime.formatTalkDate(language: Language): String =
    if (language == Language.ENGLISH) this.format(englishTalkDateFormatter) else this.format(frenchTalkDateFormatter)

fun LocalDateTime.formatTalkTime(language: Language): String =
    if (language == Language.ENGLISH) this.format(englishTalkTimeFormatter) else this.format(frenchTalkTimeFormatter)

fun LocalDateTime.toRFC3339(): String = ZonedDateTime.of(this, ZoneOffset.UTC).format(rfc3339Formatter)

private val daysLookup: Map<Long, String> =
    IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toMap(Int::toLong, ::getOrdinal))

private val frenchDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

private val englishDateFormatter = DateTimeFormatterBuilder()
    .appendPattern("MMMM")
    .appendLiteral(" ")
    .appendText(ChronoField.DAY_OF_MONTH, daysLookup)
    .appendLiteral(" ")
    .appendPattern("yyyy")
    .toFormatter(Locale.ENGLISH)

private val frenchTalkDateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

val frenchTalkTimeFormatter = DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRENCH)

private val englishTalkDateFormatter = DateTimeFormatterBuilder()
    .appendPattern("EEEE")
    .appendLiteral(" ")
    .appendPattern("MMMM")
    .appendLiteral(" ")
    .appendText(ChronoField.DAY_OF_MONTH, daysLookup)
    .toFormatter(Locale.ENGLISH)

private val englishTalkTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

private val rfc3339Formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

private fun getOrdinal(n: Int) =
    when {
        n in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }

// ----------------
// Other extensions
// ----------------
fun String.markFoundOccurrences(searchTerms: List<String> = emptyList()): String =
    if (searchTerms.isEmpty()) this else {
        var str = this
        searchTerms.forEach { str = str.replace(it, "<span class=\"mxt-text--found\">$it</span>", true) }
        str
    }

/**
 * Return true if the string contains at least one of the search terms. The search is insensitive to case.
 */
fun String.hasFoundOccurrences(searchTerms: List<String> = emptyList()): Boolean =
    this.let { str ->
        if (searchTerms.isEmpty()) false else searchTerms.any { str.contains(it, true) }
    }

fun String.stripAccents() = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

fun String.toSlug() = lowercase()
    .stripAccents()
    .replace("\n", " ")
    .replace("[^a-z\\d\\s]".toRegex(), " ")
    .split(" ")
    .joinToString("-")
    .replace("-+".toRegex(), "-") // Avoid multiple consecutive "--"

fun String.camelCase() = this
    .trim()
    .lowercase()
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.toUrlPath() = URLEncoder.encode(this, "UTF-8").replace("+", "%20")

fun String?.toPlayerUrl() = this?.let {
    if (it.startsWith("https://vimeo.com/")) {
        it.replace("https://vimeo.com/", "https://player.vimeo.com/video/")
    } else if (it.startsWith("https://www.youtube.com/watch?v=")) {
        it.replace("https://www.youtube.com/watch?v=", "https://www.youtube.com/embed/")
    } else null
}

fun String?.IsVimeoPlayer(): Boolean = this?.startsWith("https://vimeo.com/") ?: false
fun String?.IsYoutube(): Boolean = this?.contains("youtube.com") ?: false

fun String.encodeToMd5(): String? =
    if (isNullOrEmpty()) null else hex(MessageDigest.getInstance("MD5").digest(toByteArray(Charset.forName("CP1252"))))

fun String.encodeToBase64(): String? = if (isNullOrEmpty()) null else Base64.getEncoder().encodeToString(toByteArray())

fun String.decodeFromBase64(): String? =
    if (isNullOrEmpty()) null else String(Base64.getDecoder().decode(toByteArray()))

fun String?.toNumber(): Int? {
    if (isNullOrEmpty()) {
        return null
    }
    return try {
        this.toInt()
    } catch (e: NumberFormatException) {
        null
    }
}

fun String.encrypt(key: String, initVector: String): String? {
    try {
        val encrypted = cipher(key, initVector, Cipher.ENCRYPT_MODE).doFinal(toByteArray())
        return Base64.getEncoder().encodeToString(encrypted)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null
}

fun String.decrypt(key: String, initVector: String): String? {
    try {
        val encrypted = Base64.getDecoder().decode(toByteArray())
        return String(cipher(key, initVector, Cipher.DECRYPT_MODE).doFinal(encrypted))
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null
}

fun newToken(): String = UUID.randomUUID().toString().substring(0, 14).replace("-", "")

val parser: Parser by lazy { Parser.builder().extensions(listOf(AutolinkExtension.create())).build() }
val renderer: HtmlRenderer by lazy { HtmlRenderer.builder().build() }

fun String.toHTML(): String = if (this.isEmpty()) "" else renderer.render(parser.parse(this))

private fun cipher(key: String, initVector: String, mode: Int): Cipher {
    val iv = IvParameterSpec(initVector.toByteArray(charset("UTF-8")))
    val skeySpec = SecretKeySpec(key.toByteArray(charset("UTF-8")), "AES")

    val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    cipher.init(mode, skeySpec, iv)
    return cipher
}

private fun hex(digested: ByteArray): String {
    val sb = StringBuffer()
    for (i in digested.indices) {
        val v = Integer.toHexString(((digested[i].toInt() and 0xFF) or 0x100))
        sb.append(v.substring(1, 3))
    }
    return sb.toString()
}

fun <T> Iterable<T>.shuffle(): Iterable<T> =
    toMutableList().apply { this.shuffle() }

fun localePrefix(locale: Locale) = if (locale.language == "en") "/en" else ""
