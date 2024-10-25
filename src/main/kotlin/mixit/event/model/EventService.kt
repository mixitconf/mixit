package mixit.event.model

import kotlinx.coroutines.runBlocking
import mixit.event.repository.EventRepository
import mixit.user.model.CachedOrganization
import mixit.user.model.CachedSpeaker
import mixit.user.model.CachedSponsor
import mixit.user.model.CachedStaff
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.UserUpdateEvent
import mixit.util.cache.CacheCaffeineTemplate
import mixit.util.cache.CacheZone
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class EventService(
    private val repository: EventRepository,
    private val userService: UserService
) : CacheCaffeineTemplate<CachedEvent>() {

    override val cacheZone: CacheZone = CacheZone.EVENT
    override fun loader(): suspend () -> List<CachedEvent> =
        { repository.findAll().map { event -> loadEventUsers(event) } }

    suspend fun findByYear(year: Int): CachedEvent =
        findAll().first { it.year == year }

    suspend fun findByYear(year: String): CachedEvent =
        findAll().first { it.year == year.toInt() }

    fun save(event: Event) =
        repository.save(event).doOnSuccess { invalidateCache() }

    @EventListener(UserUpdateEvent::class)
    fun handleUserUpdate(userUpdateEvent: UserUpdateEvent) {
        runBlocking {
            findAll()
                .any { event ->
                    event.organizations.map { it.login }.contains(userUpdateEvent.user.login)
                    event.volunteers.map { it.login }.contains(userUpdateEvent.user.login)
                    event.organizers.map { it.login }.contains(userUpdateEvent.user.login)
                    event.sponsors.map { it.login }.contains(userUpdateEvent.user.login)
                }
                .also {
                    if (it) {
                        invalidateCache()
                    }
                }
        }
    }

    private suspend fun loadEventUsers(event: Event): CachedEvent {
        val userIds = event.organizations.map { it.organizationLogin } +
            event.sponsors.map { it.sponsorId } +
            event.volunteers.map { it.volunteerLogin } +
            event.organizers.map { it.organizerLogin }

        val users = userService.findAllByIds(userIds)

        return CachedEvent(
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
            event.organizers.map { organizer ->
                val user = users.firstOrNull { it.login == organizer.organizerLogin }
                CachedStaff(user?.toUser() ?: User())
            },
            event.photoUrls,
            event.videoUrl,
            event.schedulingFileUrl,
            event.year,
            event.streamingUrl
        )
    }
}
