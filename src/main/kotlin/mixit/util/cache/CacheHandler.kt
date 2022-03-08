package mixit.util.cache

import mixit.blog.model.BlogService
import mixit.event.handler.AdminEventHandler.Companion.TIMEZONE
import mixit.event.model.EventService
import mixit.talk.model.TalkService
import mixit.ticket.model.TicketService
import mixit.user.model.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.ZoneId

data class CacheZoneStat(
    val zone: CacheZone,
    val entries: Int,
    val refreshAt: LocalDateTime?,
    val title: String = "admin.cache.${zone.name.lowercase()}.title",
    val desc: String = "admin.cache.${zone.name.lowercase()}.desc"
) {
    companion object {
        fun <I : Cached, T : CacheTemplate<I>> init(cacheService: T): CacheZoneStat = CacheZoneStat(
            cacheService.cacheZone,
            cacheService.cache.asMap().entries.firstOrNull()?.value?.size ?: 0,
            cacheService.refreshInstant.get()?.atZone(ZoneId.of(TIMEZONE))?.toLocalDateTime()
        )
    }
}

@Component
class CacheHandler(
    private val eventService: EventService,
    private val talkService: TalkService,
    private val blogService: BlogService,
    private val userService: UserService,
    private val ticketService: TicketService
) {

    fun view(req: ServerRequest): Mono<ServerResponse> =
        ServerResponse.ok().render(
            "admin-cache",
            mapOf(
                Pair("title", "admin.cache.title"),
                Pair(
                    "zones", mapOf(
                        Pair(CacheZone.EVENT.name.lowercase(), CacheZoneStat.init(eventService)),
                        Pair(CacheZone.BLOG.name.lowercase(), CacheZoneStat.init(blogService)),
                        Pair(CacheZone.TALK.name.lowercase(), CacheZoneStat.init(talkService)),
                        Pair(CacheZone.USER.name.lowercase(), CacheZoneStat.init(userService)),
                        Pair(CacheZone.TICKET.name.lowercase(), CacheZoneStat.init(ticketService))
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
                CacheZone.TICKET -> ticketService.invalidateCache()
            }
            view(req)
        }
}