package mixit.util.cache

import java.time.LocalDateTime
import java.time.ZoneId
import mixit.blog.model.BlogService
import mixit.event.handler.AdminEventHandler.Companion.TIMEZONE
import mixit.event.model.EventService
import mixit.talk.model.TalkService
import mixit.user.model.UserService
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import mixit.util.Cached
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

data class CacheZoneStat(
    val zone: CacheZone,
    val entries: Int,
    val refreshAt: LocalDateTime,
    val title: String = "admin.cache.${zone.name.lowercase()}.title",
    val desc: String = "admin.cache.${zone.name.lowercase()}.desc"
) {
    companion object {
        fun <I : Cached, T : CacheTemplate<I>> init(cacheService: T): CacheZoneStat = CacheZoneStat(
            cacheService.cacheZone,
            cacheService.cacheList.asMap().entries.firstOrNull()?.value?.size ?: 0,
            cacheService.refreshInstant.get().atZone(ZoneId.of(TIMEZONE)).toLocalDateTime()
        )
    }
}

@Component
class CacheHandler(
    private val eventService: EventService,
    private val talkService: TalkService,
    private val blogService: BlogService,
    private val userService: UserService
) {

    fun view(req: ServerRequest): Mono<ServerResponse> =
        ServerResponse.ok().render(
            "admin-cache",
            mapOf(
                Pair("title", "admin.cache.title"),
                Pair(
                    "zones", mapOf(
                        Pair("event", CacheZoneStat.init(eventService)),
                        Pair("blog", CacheZoneStat.init(blogService)),
                        Pair("talk", CacheZoneStat.init(talkService)),
                        Pair("user", CacheZoneStat.init(userService))
                    )
                )
            )
        )

    fun invalidate(req: ServerRequest): Mono<ServerResponse> =
        CacheZone.valueOf(req.pathVariable("zone")).let {
            when (it) {
                CacheZone.TALK -> talkService.invalidateCache()
                CacheZone.BLOG -> blogService.invalidateCache()
                CacheZone.EVENT -> eventService.invalidateCache()
                CacheZone.USER -> userService.invalidateCache()
            }
            view(req)
        }
}