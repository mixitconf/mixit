package mixit.mixette.handler

import mixit.MixitProperties
import mixit.event.handler.AdminEventHandler.Companion.CURRENT_EVENT
import mixit.event.handler.AdminEventHandler.Companion.TIMEZONE
import mixit.event.model.EventService
import mixit.mixette.repository.MixetteDonationRepository
import mixit.util.frenchTalkTimeFormatter
import mixit.util.language
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import java.time.LocalTime
import java.time.ZoneId

@Component
class MixetteHandler(
    private val repository: MixetteDonationRepository,
    private val eventService: EventService,
    private val properties: MixitProperties
) {

    companion object {
        const val TEMPLATE = "mixette-dashboard"
    }

    fun mixette(req: ServerRequest): Mono<ServerResponse> =
        eventService
            .findByYear(CURRENT_EVENT.toInt())
            .map { it.organizations }
            .flatMap { organizations ->
                val donationByOrgas = repository
                    .findAllByYear(CURRENT_EVENT)
                    .collectList()
                    .map { donations ->
                        donations
                            .groupBy { donation ->
                                organizations.first { it.login == donation.organizationLogin }.let {
                                    MixetteOrganizationDonationDto(name = it.company, login = it.login)
                                }
                            }
                            .map { entry ->
                                entry.key.populate(
                                    number = entry.value.size,
                                    quantity = entry.value.sumOf { it.quantity },
                                    amount = entry.value.sumOf { it.quantity * properties.mixetteValue.toDouble() }
                                )
                            }
                    }
                ok().render(
                    TEMPLATE,
                    mapOf(
                        Pair("organizations", organizations.map { it.toSponsorDto(req.language()) }),
                        Pair("donations", donationByOrgas),
                        Pair("loadAt", LocalTime.now(ZoneId.of(TIMEZONE)).format(frenchTalkTimeFormatter)),
                        Pair("title", "mixette.dashboard.title")
                    )
                )
            }

    fun mixetteRealTime(req: ServerRequest): Mono<ServerResponse> =
        ok().contentType(MediaType.TEXT_EVENT_STREAM).body(repository.findByYearAfterNow(CURRENT_EVENT))
}
