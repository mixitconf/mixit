package mixit.event.model

import java.time.LocalDate
import mixit.user.cache.CachedOrganization
import mixit.user.cache.CachedSponsor
import mixit.user.cache.CachedStaff
import mixit.user.model.Link
import mixit.util.Cached

data class CachedEvent(
    val id: String,
    val start: LocalDate,
    val end: LocalDate,
    val current: Boolean,
    val sponsors: List<CachedSponsor>,
    val organizations: List<CachedOrganization>,
    val volunteers: List<CachedStaff>,
    val photoUrls: List<Link>,
    val videoUrl: Link?,
    val schedulingFileUrl: String?,
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
            photoUrls,
            videoUrl,
            schedulingFileUrl,
            year
        )
}
