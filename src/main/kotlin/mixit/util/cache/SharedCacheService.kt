package mixit.util.cache

import java.time.Instant
import mixit.util.CacheZone
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.ApplicationEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

data class CacheInvalidationEvent(val cacheZone: CacheZone, val instant: Instant): ApplicationEvent(cacheZone)

/**
 * When a cache invalidation event is emitted we have to send this event to all active app instances. In a real
 * world we should use a cache as Redis but we want to limit our costs for our primary need
 */
@Service
class SharedCacheService(private val client: DiscoveryClient, private val environment: Environment) {

    /**
     * Here we want to send a cache event to all service instances. In a real world we should do that with
     * a Spring Cloud Bus but we don't want to implement an AMQP broker
     */
    @EventListener
    fun handleCacheInvalidation(CacheInvalidationEvent: CacheInvalidationEvent) {
        // client.getInstances("mixit-website")[0].uri donne adresse et port
        client.getInstances(environment.getProperty("spring.application.name"))

    }

}