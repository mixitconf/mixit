package mixit.mixette.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitProperties
import mixit.event.handler.AdminEventHandler.Companion.CURRENT_EVENT
import mixit.event.handler.AdminEventHandler.Companion.TIMEZONE
import mixit.event.model.EventService
import mixit.mixette.repository.MixetteDonationRepository
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate.MixetteDashboard
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

    suspend fun mixette(req: ServerRequest): ServerResponse {
        val organizations = eventService.coFindByYear(CURRENT_EVENT).organizations
        val donationByOrgas = repository.coFindAllByYear(CURRENT_EVENT)
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
        val params = mapOf(
            "organizations" to (organizations.map { it.toSponsorDto(req.language()) }),
            "donations" to donationByOrgas,
            "loadAt" to LocalTime.now(ZoneId.of(TIMEZONE)).format(frenchTalkTimeFormatter),
            TITLE to "mixette.dashboard.title"
        )

        return ok().render(MixetteDashboard.template, params).awaitSingle()
    }

    fun mixetteRealTime(req: ServerRequest): Mono<ServerResponse> =
        ok().contentType(MediaType.TEXT_EVENT_STREAM).body(repository.findByYearAfterNow(CURRENT_EVENT))
}
