package mixit.util.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.reactor.awaitSingle
import reactor.cache.CacheMono
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

interface Cached {
    val id: String
}

enum class CacheZone { EVENT, BLOG, TALK, USER, TICKET }

/**
 * All element exposed to user (event, talk, speaker) are put in a cache. For
 * each entity we have a cache for the current year data (this cache is reloaded each hour or manually).
 * For old data and the old editions, cache is populated on startup or manually refreshed if necessary
 */
abstract class CacheTemplate<T : Cached> {

    abstract val cacheZone: CacheZone

    val refreshInstant = AtomicReference<Instant?>()

    val cache: Cache<String, List<T>> by lazy {
        Caffeine
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(5_000)
            .build()
    }

    /**
     * Cache is initialized on startup
     */
    fun initializeCache() {
        refreshInstant.set(Instant.now())
        findAll().block()
    }

    fun findOne(id: String): Mono<T> =
        findAll().flatMap { elements -> Mono.justOrEmpty(elements.firstOrNull { it.id == id }) }

    suspend fun coFindOne(id: String): T =
        findOne(id).awaitSingle()

    fun findAll(loader: () -> Mono<List<T>>): Mono<List<T>> =
        CacheMono
            .lookup({ k -> Mono.justOrEmpty(cache.getIfPresent(k)).map { Signal.next(it) } }, "global")
            .onCacheMissResume {
                loader.invoke()
            }
            .andWriteWith { key, signal ->
                Mono.fromRunnable {
                    cache.put(key, signal.get()!!)
                }
            }

    fun isEmpty(): Boolean =
        cache.asMap().entries.isEmpty()

    abstract fun findAll(): Mono<List<T>>

    fun invalidateCache() {
        cache.invalidateAll()
        refreshInstant.set(Instant.now())
    }
}
