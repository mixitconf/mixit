package mixit.util.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import mixit.util.errors.NotFoundException

interface Cached {
    val id: String
}

enum class CacheZone { EVENT, BLOG, TALK, USER, TICKET, EVENT_IMAGES, FAQ, FEATURE, FEEDBACK }

/**
 * All element exposed to user (event, talk, speaker) are put in a cache. For
 * each entity we have a cache for the current year data (this cache is reloaded each hour or manually).
 * For old data and the old editions, cache is populated on startup or manually refreshed if necessary
 */
abstract class CacheCaffeineTemplate<T : Cached> {

    abstract val cacheZone: CacheZone

    companion object {
        private const val DEFAULT_KEY = "global"
    }

    val refreshInstant = AtomicReference<Instant?>()

    private val cache: Cache<String, List<T>> by lazy {
        Caffeine
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10_000)
            .build()
    }

    /**
     * Cache is initialized on startup
     */
    suspend fun initializeCache() {
        refreshInstant.set(Instant.now())
        findAll()
    }

    abstract fun loader(): suspend () -> List<T>

    open suspend fun findOneOrNull(id: String): T? =
        findAll().firstOrNull { it.id == id }

    open suspend fun findAll(): List<T> {
        val elements = cache.getIfPresent(DEFAULT_KEY)
        if (elements.isNullOrEmpty()) {
            cache.put(DEFAULT_KEY, loader().invoke())
        }
        return cache.getIfPresent(DEFAULT_KEY) ?: throw NotFoundException()
    }

    open fun isEmpty(): Boolean =
        cache.asMap().entries.isEmpty()

    open fun invalidateCache() {
        cache.invalidateAll()
        refreshInstant.set(Instant.now())
    }

    open fun size() =
        cache.asMap().entries.firstOrNull()?.value?.size ?: 0
}
