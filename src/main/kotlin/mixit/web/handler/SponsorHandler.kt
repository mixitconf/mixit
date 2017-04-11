package mixit.web.handler

import mixit.model.*
import mixit.repository.EventRepository
import mixit.repository.UserRepository
import mixit.util.MarkdownConverter
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import java.time.LocalDate

@Component
class SponsorHandler(val userRepository: UserRepository,
                     val eventRepository: EventRepository,
                     val markdownConverter: MarkdownConverter) {

    fun viewWithSponsors(view: String, title: String?, req: ServerRequest) = eventRepository.findOne("mixit17").flatMap { event ->
        userRepository.findMany(event.sponsors.map { it.sponsorId }).collectMap(User::login).flatMap { sponsorsByLogin ->
            val sponsorsByEvent = event.sponsors.groupBy { it.level }
            ServerResponse.ok().render(view, mapOf(
                    Pair("sponsors-gold", sponsorsByEvent[SponsorshipLevel.GOLD]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                    Pair("sponsors-silver", sponsorsByEvent[SponsorshipLevel.SILVER]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                    Pair("sponsors-hosting", sponsorsByEvent[SponsorshipLevel.HOSTING]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                    Pair("sponsors-lanyard", sponsorsByEvent[SponsorshipLevel.LANYARD]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                    Pair("sponsors-party", sponsorsByEvent[SponsorshipLevel.PARTY]?.map { it.toDto(sponsorsByLogin[it.sponsorId]!!, req.language(), markdownConverter) }),
                    Pair("title", title)
            ))
    }}

}

private class EventSponsoringDto(
        val level: SponsorshipLevel,
        val sponsor: UserDto,
        val subscriptionDate: LocalDate = LocalDate.now()
)

private fun EventSponsoring.toDto(sponsor: User, language: Language, markdownConverter: MarkdownConverter) =
        EventSponsoringDto(level, sponsor.toDto(language, markdownConverter), subscriptionDate)
