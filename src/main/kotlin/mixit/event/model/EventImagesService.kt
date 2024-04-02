package mixit.event.model

import mixit.event.repository.EventImagesRepository
import mixit.util.cache.CacheCaffeineTemplate
import mixit.util.cache.CacheZone
import org.springframework.stereotype.Service

@Service
class EventImagesService(
    private val repository: EventImagesRepository
) : CacheCaffeineTemplate<CachedEventImages>() {

    override val cacheZone: CacheZone = CacheZone.EVENT_IMAGES
    override fun loader(): suspend () -> List<CachedEventImages> =
        { repository.findAll().map { CachedEventImages(it) } }

    fun save(event: EventImages) =
        repository.save(event).doOnSuccess { invalidateCache() }

    fun delete(event: String) =
        repository.delete(event).doOnSuccess { invalidateCache() }
}
