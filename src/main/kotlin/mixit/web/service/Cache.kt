package mixit.web.service

import java.time.Instant


data class CacheStatus(val size: Int, val lastRefresh: Instant)

interface Cache<Key, Value> {

    fun keys(): List<Key>
    fun values(): List<Value>
    fun lastRefresh(): Instant

    fun status() = CacheStatus(values().size, lastRefresh())

    /**
     * Clear all entries and reload all values from DB
     */
    fun invalidate()

    /**
     * Update only one entry in the cache
     */
    fun invalidateEntry(key: Key, value: Value)

    fun findById(key: Key): Value

    fun findAllByYear(year: Int)
}
