package mixit.util

import com.mongodb.client.result.DeleteResult
import mixit.model.Language
import org.springframework.boot.SpringApplication
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.*
import org.springframework.web.reactive.function.server.RouterDsl
import org.springframework.web.reactive.function.server.Routes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.status
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

inline fun <reified T : Any> ReactiveMongoOperations.findById(id: Any): Mono<T> = findById(id, T::class.java)

inline fun <reified T : Any> ReactiveMongoOperations.find(query: Query): Flux<T> = find(query, T::class.java)

inline fun <reified T : Any> ReactiveMongoOperations.findAll(): Flux<T> = findAll(T::class.java)

inline fun <reified T : Any> ReactiveMongoOperations.findOne(query: Query): Mono<T> = find(query, T::class.java).next()

inline fun <reified T : Any> ReactiveMongoOperations.remove(query: Query): Mono<DeleteResult> = remove(query, T::class.java)

inline fun <reified T : Any> ReactiveMongoOperations.count(): Mono<Long> = count(Query(), T::class.java)

// -------------------------
// Spring WebFlux extensions
// -------------------------

fun router(routes: Routes) = RouterDsl().apply(routes).router()

fun ServerRequest.language() = Language.findByTag(this.headers().header(HttpHeaders.ACCEPT_LANGUAGE).first())

fun ServerResponse.BodyBuilder.json() = contentType(APPLICATION_JSON_UTF8)

fun ServerResponse.BodyBuilder.xml() = contentType(APPLICATION_XML)

fun ServerResponse.BodyBuilder.html() = contentType(TEXT_HTML)

fun permanentRedirect(uri: String) = ServerResponse.permanentRedirect(URI(uri)).build()

fun temporaryRedirect(uri: String) = ServerResponse.temporaryRedirect(URI(uri)).build()

fun found(uri: String): Mono<ServerResponse> {
    val builder = status(HttpStatus.FOUND)
    return builder.location(URI(uri)).build()
}

// --------------------
// Date/Time extensions
// --------------------

fun LocalDateTime.format(language: Language): String =
        if (language == Language.ENGLISH) this.format(englishDateFormatter) else this.format(frenchDateFormatter)

private val daysLookup: Map<Long, String> =
        IntStream.rangeClosed(1, 31).boxed().collect(Collectors.toMap(Int::toLong, ::getOrdinal))

private val frenchDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

private val englishDateFormatter = DateTimeFormatterBuilder()
        .appendPattern("MMMM")
        .appendLiteral(" ")
        .appendText(ChronoField.DAY_OF_MONTH, daysLookup)
        .appendLiteral(" ")
        .appendPattern("yyyy").toFormatter(Locale.ENGLISH)


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

fun String.toSlug() =
        toLowerCase()
                .stripAccents()
                .replace("\n", " ")
                .replace("[^a-z\\d\\s]".toRegex(), " ")
                .split(" ")
                .joinToString("-")

fun <T> Iterable<T>.shuffle(): Iterable<T> =
        toMutableList().apply { Collections.shuffle(this) }
