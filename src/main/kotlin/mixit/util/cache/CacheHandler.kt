package mixit.util.cache

import kotlinx.coroutines.reactor.awaitSingle
import mixit.blog.model.BlogService
import mixit.event.handler.AdminEventHandler.Companion.TIMEZONE
import mixit.event.model.EventService
import mixit.routes.MustacheI18n
import mixit.routes.MustacheTemplate.AdminCache
import mixit.talk.model.TalkService
import mixit.ticket.model.TicketService
import mixit.user.model.UserService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
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

    suspend fun view(req: ServerRequest): ServerResponse {
        val params = mapOf(
            MustacheI18n.TITLE to "admin.cache.title",
            "zones" to mapOf(
                CacheZone.EVENT.name.lowercase() to CacheZoneStat.init(eventService),
                CacheZone.BLOG.name.lowercase() to CacheZoneStat.init(blogService),
                CacheZone.TALK.name.lowercase() to CacheZoneStat.init(talkService),
                CacheZone.USER.name.lowercase() to CacheZoneStat.init(userService),
                CacheZone.TICKET.name.lowercase() to CacheZoneStat.init(ticketService)
            )
        )

        return ServerResponse.ok().render(AdminCache.template, params).awaitSingle()
    }

    suspend fun invalidate(req: ServerRequest): ServerResponse =
        CacheZone
            .valueOf(req.pathVariable("zone"))
            .let {
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
