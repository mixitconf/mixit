package mixit.event.model

import mixit.MixitApplication.Companion.speakerStarInCurrentEvent
import mixit.MixitApplication.Companion.speakerStarInHistory
import mixit.event.repository.EventRepository
import mixit.user.model.CachedOrganization
import mixit.user.model.CachedSpeaker
import mixit.user.model.CachedSponsor
import mixit.user.model.CachedStaff
import mixit.user.repository.UserRepository
import mixit.util.CacheTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) : CacheTemplate<CachedEvent>() {

    override fun findAll(): Flux<CachedEvent> =
        findAll { eventRepository.findAll().flatMap { event -> loadEventUsers(event) } }

    fun findByYear(year: Int): Mono<CachedEvent> =
        findAll().collectList().flatMap { events -> Mono.justOrEmpty(events.firstOrNull { it.year == year }) }

    fun save(event: Event) =
        eventRepository.save(event).also { cacheList.invalidateAll() }


    private fun loadEventUsers(event: Event): Mono<CachedEvent> {
        val userIds = event.organizations.map { it.organizationLogin } +
                event.sponsors.map { it.sponsorId } +
                event.volunteers.map { it.volunteerLogin } +
                speakerStarInHistory +
                speakerStarInCurrentEvent

        return userRepository.findAllByIds(userIds).collectList().map { users ->
            CachedEvent(
                event.id,
                event.start,
                event.end,
                event.current,
                event.sponsors.map { eventSponsoring ->
                    val sponsor = users.first { it.login == eventSponsoring.sponsorId }
                    CachedSponsor(sponsor, eventSponsoring)
                },
                event.organizations.map { orga ->
                    CachedOrganization(users.first { it.login == orga.organizationLogin })
                },
                event.volunteers.map { volunteer ->
                    CachedStaff(users.first { it.login == volunteer.volunteerLogin })
                },
                event.photoUrls,
                event.videoUrl,
                event.schedulingFileUrl,
                speakerStarInHistory.map { starLogin ->
                    CachedSpeaker(users.first { it.login == starLogin })
                },
                speakerStarInCurrentEvent.map { starLogin ->
                    CachedSpeaker(users.first { it.login == starLogin })
                },
                event.year
            )
        }
    }
}