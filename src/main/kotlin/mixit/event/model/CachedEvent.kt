package mixit.event.model

import mixit.talk.model.Language
import mixit.user.model.CachedOrganization
import mixit.user.model.CachedSpeaker
import mixit.user.model.CachedSponsor
import mixit.user.model.CachedStaff
import mixit.user.model.Link
import mixit.util.cache.Cached
import mixit.util.formatDate
import java.time.LocalDate

data class CachedEvent(
    override val id: String,
    val start: LocalDate,
    val end: LocalDate,
    val current: Boolean,
    val sponsors: List<CachedSponsor>,
    val organizations: List<CachedOrganization>,
    val volunteers: List<CachedStaff>,
    val organizers: List<CachedStaff>,
    val photoUrls: List<Link>,
    val videoUrl: Link?,
    val schedulingFileUrl: String?,
    val speakerStarInHistory: List<CachedSpeaker>,
    val speakerStarInCurrent: List<CachedSpeaker>,
    val year: Int
) : Cached {
    fun toEvent(): Event =
        Event(
            id,
            start,
            end,
            current,
            sponsors.map { s -> EventSponsoring(s.level, s.login, s.subscriptionDate) },
            organizations.map { o -> EventOrganization(o.login) },
            volunteers.map { v -> EventVolunteer(v.login) },
            organizers.map { v -> EventOrganizer(v.login) },
            photoUrls,
            videoUrl,
            schedulingFileUrl,
            year
        )

    fun toEventDto(language: Language): EventDto =
        EventDto(
            id,
            start.formatDate(language),
            end.formatDate(language),
            current,
            photoUrls,
            videoUrl?.copy(url = videoUrl.url.replace("https://vimeo.com", "https://player.vimeo.com/video")),
            schedulingFileUrl,
            year
        )
    fun filterBySponsorLevel(vararg levels: SponsorshipLevel): List<CachedSponsor> =
        sponsors.filter { levels.contains(it.level) }
}
