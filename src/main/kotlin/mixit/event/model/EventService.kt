package mixit.event.model

import kotlinx.coroutines.reactor.awaitSingle
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
import mixit.util.cache.CacheTemplate
import mixit.util.cache.CacheZone
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Duration

@Service
class EventService(
    private val repository: EventRepository,
    private val userService: UserService
) : CacheTemplate<CachedEvent>() {

    override val cacheZone: CacheZone = CacheZone.EVENT

    override fun findAll(): Mono<List<CachedEvent>> =
        findAll { repository.findAll().flatMap { event -> loadEventUsers(event) }.collectList() }

    suspend fun coFindAll(): List<CachedEvent> =
        findAll().awaitSingle()

    fun findByYear(year: Int): Mono<CachedEvent> =
        findAll().map { events -> events.first { it.year == year } }

    suspend fun coFindByYear(year: Int): CachedEvent =
        findByYear(year).awaitSingle()

    suspend fun coFindByYear(year: String): CachedEvent =
        findByYear(year.toInt()).awaitSingle()

    fun save(event: Event) =
        repository.save(event).doOnSuccess { cache.invalidateAll() }

    @EventListener
    fun handleUserUpdate(userUpdateEvent: UserUpdateEvent) {
        findAll()
            .switchIfEmpty { Mono.just(emptyList()) }
            .map { events ->
                events.any { event ->
                    event.organizations.map { it.login }.contains(userUpdateEvent.user.login)
                    event.volunteers.map { it.login }.contains(userUpdateEvent.user.login)
                    event.sponsors.map { it.login }.contains(userUpdateEvent.user.login)
                }
            }
            .switchIfEmpty { Mono.just(true) }
            .block(Duration.ofSeconds(10))
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
            speakerStarInHistory.map { it.login } +
            speakerStarInCurrentEvent.map { it.login }

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
                    val user = users.firstOrNull { it.login == starLogin.login }?.toUser() ?: User()
                    CachedSpeaker(user, starLogin.year)
                },
                speakerStarInCurrentEvent.map { starLogin ->
                    val user = users.firstOrNull { it.login == starLogin.login }?.toUser() ?: User()
                    CachedSpeaker(user, starLogin.year)
                },
                event.year
            )
        }
    }
}
