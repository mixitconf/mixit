package mixit.util

import mixit.model.Language
import org.springframework.boot.SpringApplication
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.*
import java.net.URI
import java.text.Normalizer
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.reflect.KClass

// ----------------------
// Spring Boot extensions
// ----------------------

fun run(type: KClass<*>, vararg args: String) = SpringApplication.run(type.java, *args)

// -------------------------
// Spring WebFlux extensions
// -------------------------

fun ServerRequest.language() =
        Language.findByTag(this.headers().asHttpHeaders().acceptLanguageAsLocales.first().language)

fun ServerRequest.locale() =
        this.headers().asHttpHeaders().acceptLanguageAsLocales.first() ?: Locale.ENGLISH

fun ServerResponse.BodyBuilder.json() = contentType(APPLICATION_JSON_UTF8)

fun ServerResponse.BodyBuilder.xml() = contentType(APPLICATION_XML)

fun ServerResponse.BodyBuilder.html() = contentType(TEXT_HTML)

fun permanentRedirect(uri: String) = permanentRedirect(URI(uri)).build()

fun seeOther(uri: String) = seeOther(URI(uri)).build()

// --------------------
// Date/Time extensions
// --------------------

fun LocalDateTime.formatDate(language: Language): String =
        if (language == Language.ENGLISH) this.format(englishDateFormatter) else this.format(frenchDateFormatter)

fun LocalDateTime.formatTalkDate(language: Language): String =
        if (language == Language.ENGLISH) this.format(englishTalkDateFormatter) else this.format(frenchTalkDateFormatter).capitalize()

fun LocalDateTime.formatTalkTime(language: Language): String =
        if (language == Language.ENGLISH) this.format(englishTalkTimeFormatter) else this.format(frenchTalkTimeFormatter)

fun LocalDateTime.toRFC3339(): String = ZonedDateTime.of(this, ZoneOffset.UTC) .format(rfc3339Formatter)


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

private val frenchTalkTimeFormatter = DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRENCH)

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

fun String.stripAccents() = Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

fun String.toSlug() = toLowerCase()
        .stripAccents()
        .replace("\n", " ")
        .replace("[^a-z\\d\\s]".toRegex(), " ")
        .split(" ")
        .joinToString("-")
        .replace("-+".toRegex(), "-")   // Avoid multiple consecutive "--"

fun <T> Iterable<T>.shuffle(): Iterable<T> =
        toMutableList().apply { Collections.shuffle(this) }

fun localePrefix(locale: Locale) = if (locale.language == "en") "/en" else ""



