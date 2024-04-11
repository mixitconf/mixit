package mixit.event.model

import java.time.LocalDate
import mixit.user.model.CachedOrganization
import mixit.user.model.CachedSpeaker
import mixit.user.model.CachedSponsor
import mixit.user.model.CachedStaff
import mixit.user.model.Link
import mixit.util.cache.Cached

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
    val year: Int,
    val streamingUrl: String?
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
            year,
            streamingUrl
        )

    fun filterBySponsorLevel(vararg levels: SponsorshipLevel): List<CachedSponsor> =
        sponsors.filter { levels.contains(it.level) }
}
