package mixit.user.handler

import mixit.event.model.EventService
import mixit.event.model.SponsorshipLevel
import mixit.event.model.SponsorshipLevel.ACCESSIBILITY
import mixit.event.model.SponsorshipLevel.ECOLOGY
import mixit.event.model.SponsorshipLevel.GOLD
import mixit.event.model.SponsorshipLevel.HOSTING
import mixit.event.model.SponsorshipLevel.LANYARD
import mixit.event.model.SponsorshipLevel.MIXTEEN
import mixit.event.model.SponsorshipLevel.PARTY
import mixit.event.model.SponsorshipLevel.SILVER
import mixit.event.model.SponsorshipLevel.VIDEO
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

@Component
class SponsorHandler(private val eventService: EventService) {

    fun viewWithSponsors(year: Int, req: ServerRequest) =
        eventService
            .findByYear(year)
            .flatMap { event ->
                val lg = req.language()
                val goldSponsors = event.filterBySponsorLevel(GOLD).map { it.toEventSponsoringDto(lg) }
                val silverSponsors = event.filterBySponsorLevel(SILVER).map { it.toEventSponsoringDto(lg) }
                val hostSponsors = event.filterBySponsorLevel(HOSTING).map { it.toEventSponsoringDto(lg) }
                val ecoSponsors = event.filterBySponsorLevel(ECOLOGY).map { it.toEventSponsoringDto(lg) }
                val lanyardSponsors = event.filterBySponsorLevel(LANYARD).map { it.toEventSponsoringDto(lg) }
                val accessSponsors = event.filterBySponsorLevel(ACCESSIBILITY).map { it.toEventSponsoringDto(lg) }
                val mixteenSponsors = event.filterBySponsorLevel(MIXTEEN).map { it.toEventSponsoringDto(lg) }
                val partySponsors = event.filterBySponsorLevel(PARTY).map { it.toEventSponsoringDto(lg) }
                val videoSponsors = event.filterBySponsorLevel(VIDEO).map { it.toEventSponsoringDto(lg) }

                ServerResponse.ok().render(
                    "sponsors",
                    mapOf(
                        Pair("year", year),
                        Pair("sponsors-gold", goldSponsors),
                        Pair("has-sponsors-gold", goldSponsors.isNotEmpty()),
                        Pair("sponsors-silver", silverSponsors),
                        Pair("has-sponsors-silver", silverSponsors.isNotEmpty()),
                        Pair("sponsors-hosting", hostSponsors),
                        Pair("has-sponsors-hosting", hostSponsors.isNotEmpty()),
                        Pair("sponsors-ecology", ecoSponsors),
                        Pair("has-sponsors-ecology", ecoSponsors.isNotEmpty()),
                        Pair("sponsors-lanyard", lanyardSponsors),
                        Pair("has-sponsors-lanyard", lanyardSponsors.isNotEmpty()),
                        Pair("sponsors-accessibility", accessSponsors),
                        Pair("has-sponsors-accessibility", accessSponsors.isNotEmpty()),
                        Pair("sponsors-mixteen", mixteenSponsors),
                        Pair("has-sponsors-mixteen", mixteenSponsors.isNotEmpty()),
                        Pair("sponsors-party", partySponsors),
                        Pair("has-sponsors-party", partySponsors.isNotEmpty()),
                        Pair("sponsors-video", videoSponsors),
                        Pair("has-sponsors-video", videoSponsors.isNotEmpty()),
                        Pair("title", "sponsors.title|$year")
                    )
                )
            }

    fun viewWithSponsors(
        view: String,
        spotLights: Array<SponsorshipLevel>,
        title: String?,
        year: Int,
        req: ServerRequest
    ) =
        eventService
            .findByYear(year)
            .flatMap { event ->
                val mainSponsors = event.filterBySponsorLevel(*spotLights)
                val otherSponsors = event.sponsors.filterNot { mainSponsors.contains(it) }

                val context = mutableMapOf(
                    Pair("year", year),
                    Pair("title", if (view != "sponsors") title else "$title|$year"),
                    Pair("sponsors-main", mainSponsors.map { it.toSponsorDto() }),
                    Pair("sponsors-others", otherSponsors.map { it.toSponsorDto() })
                )
                if (view == "home") {
                    context["stars-old"] = event.speakerStarInHistory
                        .shuffled()
                        .map { it.toSpeakerStarDto() }
                        .subList(0, 6)
                }

                ServerResponse.ok().render(view, context)
            }
}


