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
                                    Pair("sponsors-gold", sponsorsByEvent[SponsorshipLevel.GOLD]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-silver", sponsorsByEvent[SponsorshipLevel.SILVER]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-hosting", sponsorsByEvent[SponsorshipLevel.HOSTING]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-lanyard", sponsorsByEvent[SponsorshipLevel.LANYARD]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-accessibility", sponsorsByEvent[SponsorshipLevel.ACCESSIBILITY]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-mixteen", sponsorsByEvent[SponsorshipLevel.MIXTEEN]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-party", sponsorsByEvent[SponsorshipLevel.PARTY]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("sponsors-video", sponsorsByEvent[SponsorshipLevel.VIDEO]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                                    Pair("title", "sponsors.title|$year")
                            ))
                        }
                    }

    fun viewWithSponsors(view: String, spolight: SponsorshipLevel, title: String?, year: Int, req: ServerRequest) =
            eventRepository
                    .findByYear(year)
                    .flatMap { event ->
                        userRepository
                                .findMany(event.sponsors.map { it.sponsorId })
                                .collectMap(User::login)
                                .flatMap { sponsorsByLogin ->
                                    val sponsorsByEvent = event.sponsors.groupBy { it.level }

                                    ServerResponse.ok().render(view, mapOf(
                                            Pair("year", year),
                                            Pair("imagepath", "/"),
                                            Pair("title", if (!view.equals("sponsors")) title else "$title|$year"),
                                            Pair("sponsors-main", sponsorsByEvent[spolight]?.map { it.toSponsorDto(sponsorsByLogin[it.sponsorId]!!) }),
                                            Pair("sponsors-others", event.sponsors
                                                    .filter { it.level != SponsorshipLevel.GOLD }
                                                    .map { it.toSponsorDto(sponsorsByLogin[it.sponsorId]!!) }
                                                    .distinctBy { it.login })
                                    ))
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
        val logoWebpUrl: String? = null
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
