package mixit.util

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import reactor.cache.CacheFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import reactor.core.publisher.SignalType


interface Cached

/**
 * All element exposed to user (event, talk, speaker) are put in a cache. For
 * each entity we have a cache for the current year data (this cache is reloaded each hour or manually).
 * For old data and the old editions, cache is populated on startup or manually refreshed if necessary
 */
abstract class CacheTemplate<T : Cached> {

    val cacheList: Cache<String, MutableList<T>> by lazy {
        Caffeine
            .newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(2_500)
            .build()
    }

    fun findAll(loader: (String) -> Flux<T>): Flux<T> =
        CacheFlux
            .lookup({ k ->
                val cached = cacheList.getIfPresent(k)
                if (cached == null) {
                    Mono.empty()
                } else {
                    Mono.just(cached).flatMapMany { Flux.fromIterable(it) }.map { Signal.next(it) }.collectList()
                }
            }, "global")
            .onCacheMissResume {
                loader.invoke("global")
            }
            .andWriteWith { key, signal ->
                Mono.fromRunnable {
                    cacheList.put(
                        key,
                        signal.filter { it.type == SignalType.ON_NEXT }.map { it.get()!! }.toMutableList()
                    )
                }
            }
}

