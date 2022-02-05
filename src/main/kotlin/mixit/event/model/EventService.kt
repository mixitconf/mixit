package mixit.event.model

import mixit.MixitApplication.Companion.speakerStarInCurrentEvent
import mixit.MixitApplication.Companion.speakerStarInHistory
import mixit.event.repository.EventRepository
import mixit.user.model.CachedOrganization
import mixit.user.model.CachedSpeaker
import mixit.user.model.CachedSponsor
import mixit.user.model.CachedStaff
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.UserUpdateEvent
import mixit.util.CacheTemplate
import mixit.util.CacheZone
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventService(
    eventPublisher: ApplicationEventPublisher,
    private val repository: EventRepository,
    private val userService: UserService
) : CacheTemplate<CachedEvent>(eventPublisher) {

    override val cacheZone: CacheZone = CacheZone.EVENT

    override fun findAll(): Flux<CachedEvent> =
        findAll { repository.findAll().flatMap { event -> loadEventUsers(event) } }

    fun findByYear(year: Int): Mono<CachedEvent> =
        findAll().collectList().map { events -> events.first { it.year == year } }

    fun save(event: Event) =
        repository.save(event).doOnSuccess { cacheList.invalidateAll() }

    fun invalidateCacheIfUserFound(user: User): Mono<Boolean> =
        findAll().collectList().map { events ->
            events.any { event ->
                event.organizations.map { it.login }.contains(user.login)
                event.volunteers.map { it.login }.contains(user.login)
                event.sponsors.map { it.login }.contains(user.login)
            }.also {
                if (it) {
                    invalidateCache()
                }
            }
        }

    @EventListener
    fun handleUserUpdate(userUpdateEvent: UserUpdateEvent) {
        findAll()
            .collectList()
            .map { events ->
                events.any { event ->
                    event.organizations.map { it.login }.contains(userUpdateEvent.user.login)
                    event.volunteers.map { it.login }.contains(userUpdateEvent.user.login)
                    event.sponsors.map { it.login }.contains(userUpdateEvent.user.login)
                }
            }
            .block()
            .also {
                if (it != null && it) {
                    invalidateCache()
                }
            }
    }

    private fun loadEventUsers(event: Event): Mono<CachedEvent> {
        val userIds = event.organizations.map { it.organizationLogin } +
                event.sponsors.map { it.sponsorId } +
                event.volunteers.map { it.volunteerLogin } +
                speakerStarInHistory +
                speakerStarInCurrentEvent

        return userService.findAllByIds(userIds).map { users ->
            CachedEvent(
                event.id,
                event.start,
                event.end,
                event.current,
                event.sponsors.map { eventSponsoring ->
                    val sponsor = users.firstOrNull { it.login == eventSponsoring.sponsorId }
                    CachedSponsor(sponsor?.toUser() ?: User(), eventSponsoring)
                },
                event.organizations.map { orga ->
                    val user = users.firstOrNull { it.login == orga.organizationLogin }
                    CachedOrganization(user?.toUser() ?: User())
                },
                event.volunteers.map { volunteer ->
                    val user = users.firstOrNull { it.login == volunteer.volunteerLogin }
                    CachedStaff(user?.toUser() ?: User())
                },
                event.photoUrls,
                event.videoUrl,
                event.schedulingFileUrl,
                speakerStarInHistory.map { starLogin ->
                    CachedSpeaker(users.firstOrNull { it.login == starLogin }?.toUser() ?: User())
                },
                speakerStarInCurrentEvent.map { starLogin ->
                    CachedSpeaker(users.firstOrNull { it.login == starLogin }?.toUser() ?: User())
                },
                event.year
            )
        }
    }
}