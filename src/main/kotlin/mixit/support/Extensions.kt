package mixit.support

import com.mongodb.client.result.DeleteResult
import mixit.model.Language
import org.springframework.boot.SpringApplication
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.text.Normalizer
import java.time.LocalDateTime
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

// ----------------------
// Spring Data extensions
// ----------------------

inline fun <reified T : Any> ReactiveMongoOperations.findById(id: Any) : Mono<T> = findById(id, T::class.java)

fun <T : Any> ReactiveMongoOperations.findById(id: Any, type: KClass<T>) : Mono<T> = findById(id, type.java)

inline fun <reified T : Any> ReactiveMongoOperations.find(query: Query) : Flux<T> = find(query, T::class.java)

fun <T : Any> ReactiveMongoOperations.findAll(type: KClass<T>) : Flux<T> = findAll(type.java)

fun <T : Any> ReactiveMongoOperations.find(query: Query, type: KClass<T>) : Flux<T> = find(query, type.java)

inline fun <reified T : Any> ReactiveMongoOperations.findOne(query: Query) : Mono<T> = find(query, T::class.java).next()

fun ReactiveMongoOperations.remove(query: Query, type: KClass<*>): Mono<DeleteResult> = remove(query, type.java)

// -------------------------
// Spring WebFlux extensions
// -------------------------

fun ServerRequest.language() = Language.findByTag(this.headers().header(HttpHeaders.ACCEPT_LANGUAGE).first())

fun ServerResponse.BodyBuilder.json() = contentType(APPLICATION_JSON_UTF8)

fun ServerResponse.BodyBuilder.xml() = contentType(APPLICATION_XML)

fun ServerResponse.BodyBuilder.html() = contentType(TEXT_HTML)

fun permanentRedirect(uri: String) = ServerResponse.permanentRedirect(URI(uri)).build()

// --------------------
// Date/Time extensions
// --------------------

fun LocalDateTime.format(language: Language) : String =
        if (language == Language.ENGLISH) this.format(englishDateFormatter) else this.format(frenchDateFormatter)

private val daysLookup : Map<Long, String> =
        IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toMap(Int::toLong, ::getOrdinal))

private val frenchDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

private val englishDateFormatter = DateTimeFormatterBuilder()
        .appendPattern("MMMM")
        .appendLiteral(" ")
        .appendText(ChronoField.DAY_OF_MONTH, daysLookup)
        .appendLiteral(" ")
        .appendPattern("yyyy").toFormatter(Locale.ENGLISH)

private fun getOrdinal(n: Int): String {
        if (n >= 11 && n <= 13) {
            return n.toString() + "th"
        }
        when (n % 10) {
            1 -> return n.toString() + "st"
            2 -> return n.toString() + "nd"
            3 -> return n.toString() + "rd"
            else -> return n.toString() + "th"
        }
    }

// ----------------
// Other extensions
// ----------------

fun String.stripAccents() = Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

fun String.toSlug() = this.toLowerCase()
            .stripAccents()
            .replace("\n", " ")
            .replace("[^a-z\\d\\s]".toRegex(), " ")
            .split(" ")
            .joinToString("-")

fun <T> Iterable<T>.shuffle(): Iterable<T> {
    val shuffledList = this.toMutableList()
    Collections.shuffle(shuffledList)
    return shuffledList
}

