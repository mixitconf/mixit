package mixit.util.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit

/**
 * All element exposed to user (event, talk, speaker) are put in a cache. For
 * each entity we have a cache for the current year data (this cache is reloaded each hour or manually).
 * For old data and the old editions, cache is populated on startup or manually refreshed if necessary
 */
abstract class CacheCaffeineUnitaryTemplate<T : Cached> : CacheCaffeineTemplate<T>() {

    private val cache: Cache<String, T> by lazy {
        Caffeine
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10_000)
            .build()
    }

    abstract fun unitaryLoader(id: String): suspend () -> T?

    override suspend fun findAll(): List<T> {
        if (isEmpty()) {
            loader().invoke().onEach {
                cache.put(it.id, it)
            }
        }
        return cache.asMap().values.toList()
    }


    override suspend fun findOneOrNull(id: String): T? {
        val elt = cache.getIfPresent(id)
        if(elt == null) {
            unitaryLoader(id).invoke()?.also {
                cache.put(id, it)
            }
        }
        return cache.getIfPresent(id)
    }

    suspend fun updateElement(elt: T) {
        findOneOrNull(elt.id)?.also {
            cache.invalidate(elt.id)
        }
        cache.put(elt.id, elt)
    }

    fun invalidateElement(id: String) {
        cache.invalidate(id)
    }

    override fun size() =
        cache.asMap().entries.size
}
