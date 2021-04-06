package mixit.web.handler

import mixit.model.EventSponsoring
import mixit.model.Language
import mixit.model.SponsorshipLevel
import mixit.model.User
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import java.time.LocalDate
import java.util.*

@Component
class SponsorHandler(private val userRepository: UserRepository,
                     private val eventRepository: EventRepository,
                     private val markdownConverter: MarkdownConverter) {

    fun viewWithSponsors(year: Int, req: ServerRequest) =
            eventRepository
                    .findByYear(year)
                    .flatMap { event ->
                        userRepository.findMany(event.sponsors.map { it.sponsorId }).collectMap(User::login).flatMap { sponsorsByLogin ->
                            val sponsorsByEvent = event.sponsors.groupBy { it.level }
                            ServerResponse.ok().render("sponsors", mapOf(
                                    Pair("year", year),
                                    Pair("imagepath", "/"),
                                    Pair("sponsors-gold", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.GOLD)),
                                    Pair("has-sponsors-gold", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.GOLD)?.isNotEmpty()),
                                    Pair("sponsors-silver", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.SILVER)),
                                    Pair("has-sponsors-silver", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.SILVER)?.isNotEmpty()),
                                    Pair("sponsors-hosting", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.HOSTING)),
                                    Pair("has-sponsors-hosting", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.HOSTING)?.isNotEmpty()),
                                    Pair("sponsors-ecology", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.ECOLOGY)),
                                    Pair("has-sponsors-ecology", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.ECOLOGY)?.isNotEmpty()),
                                    Pair("sponsors-lanyard", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.LANYARD)),
                                    Pair("has-sponsors-lanyard", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.LANYARD)?.isNotEmpty()),
                                    Pair("sponsors-accessibility", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.ACCESSIBILITY)),
                                    Pair("has-sponsors-accessibility", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.ACCESSIBILITY)?.isNotEmpty()),
                                    Pair("sponsors-mixteen", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.MIXTEEN)),
                                    Pair("has-sponsors-mixteen", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.MIXTEEN)?.isNotEmpty()),
                                    Pair("sponsors-party", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.PARTY)),
                                    Pair("has-sponsors-party", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.PARTY)?.isNotEmpty()),
                                    Pair("sponsors-video", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.VIDEO)),
                                    Pair("has-sponsors-video", getSponsors(sponsorsByEvent, sponsorsByLogin, req.language(), SponsorshipLevel.VIDEO)?.isNotEmpty()),
                                    Pair("title", "sponsors.title|$year")
                            ))
                        }
                    }

    private fun getSponsors(sponsorsByEvent: Map<SponsorshipLevel, List<EventSponsoring>>,
                            sponsorsByLogin: Map<String, User>,
                            language: Language,
                            sponsorshipLevel: SponsorshipLevel): List<EventSponsoringDto>? =
            sponsorsByEvent[sponsorshipLevel]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, language, markdownConverter) }

    fun viewWithSponsors(view: String, spolights: Array<SponsorshipLevel>, title: String?, year: Int, req: ServerRequest) =
            eventRepository
                    .findByYear(year)
                    .flatMap { event ->
                        val isHomePage = view.equals("home")

                        val ids = event.sponsors.map { it.sponsorId }.toMutableList()

                        if (isHomePage) {
                            // We adds speaker stars
                            ids.addAll(UserHandler.speakerStarInCurrentEvent)
                            ids.addAll(UserHandler.speakerStarInHistory)
                        }

                        userRepository
                                .findMany(ids)
                                .collectMap(User::login)
                                .flatMap { usersByLogin ->
                                    val sponsorsByEvent = event.sponsors.groupBy { it.level }
                                    val mainSponsors = mutableListOf<SponsorDto>()

                                    spolights.forEach {
                                        val elements = sponsorsByEvent[it]?.map { it.toSponsorDto(usersByLogin[it.sponsorId]!!) }?.toList()
                                        mainSponsors.addAll(elements.orEmpty())
                                    }

                                    val mainSponsorIds = mainSponsors.map { it.login }

                                    val otherSponsors = event.sponsors
                                            .filter { !spolights.contains(it.level) && !mainSponsorIds.contains(it.sponsorId)}
                                            .map { it.toSponsorDto(usersByLogin[it.sponsorId]!!) }
                                            .distinctBy { it.login }

                                    if (view.equals("home")) {
                                        // val oldStars = UserHandler.speakerStarInHistory.map { usersByLogin[it]!!.toSpeakerStarDto() }.toMutableList()
                                        // val currentStars = UserHandler.speakerStarInCurrentEvent.map { usersByLogin[it]!!.toSpeakerStarDto() }.toMutableList()
                                        // Collections.shuffle(oldStars)
                                        // Collections.shuffle(currentStars)

                                        ServerResponse.ok().render(view, mapOf(
                                                Pair("year", year),
                                                Pair("imagepath", "/"),
                                                Pair("title", if (!view.equals("sponsors")) title else "$title|$year"),
                                                Pair("sponsors-main", mainSponsors),
                                                Pair("sponsors-others", otherSponsors)
                                                // Pair("stars-old", oldStars.subList(0, 6)),
                                                // Pair("stars-current", currentStars)
                                        ))
                                    } else {
                                        ServerResponse.ok().render(view, mapOf(
                                                Pair("year", year),
                                                Pair("imagepath", "/"),
                                                Pair("title", if (!view.equals("sponsors")) title else "$title|$year"),
                                                Pair("sponsors-main", mainSponsors),
                                                Pair("sponsors-others", otherSponsors)
                                        ))
                                    }
                                }
                    }
}

class EventSponsoringDto(
        val level: SponsorshipLevel,
        val sponsor: UserDto,
        val subscriptionDate: LocalDate = LocalDate.now()
)

class SponsorDto(
        val login: String,
        var company: String,
        var photoUrl: String,
        val logoType: String?,
        val logoWebpUrl: String? = null,
        val isAbsoluteLogo: Boolean = photoUrl.startsWith("http")
)

fun EventSponsoring.toDto(sponsor: User, language: Language, markdownConverter: MarkdownConverter) =
        EventSponsoringDto(level, sponsor.toDto(language, markdownConverter), subscriptionDate)

fun EventSponsoring.toSponsorDto(sponsor: User) =
        SponsorDto(
                sponsor.login,
                sponsor.company!!,
                sponsor.photoUrl!!,
                logoType(sponsor.photoUrl),
                logoWebpUrl(sponsor.photoUrl)
        )
