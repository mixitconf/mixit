package mixit.mixette.handler

import java.math.BigDecimal
import java.time.Duration.ofMillis
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitApplication.Companion.TIMEZONE
import mixit.MixitProperties
import mixit.event.model.EventService
import mixit.mixette.model.MixetteDonation
import mixit.mixette.repository.MixetteDonationRepository
import mixit.security.model.Cryptographer
import mixit.talk.model.Language
import mixit.ticket.model.CachedTicket
import mixit.ticket.model.TicketService
import mixit.user.model.CachedUser
import mixit.user.model.UserService
import mixit.util.frenchTalkTimeFormatter
import mixit.util.language
import mixit.util.mustache.MustacheI18n.TITLE
import mixit.util.mustache.MustacheTemplate.MixetteDashboard
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.renderAndAwait
import reactor.core.publisher.Flux

@Component
class MixetteHandler(
    private val repository: MixetteDonationRepository,
    private val eventService: EventService,
    private val ticketService: TicketService,
    private val userService: UserService,
    private val properties: MixitProperties,
    private val cryptographer: Cryptographer
) {

    suspend fun mixette(req: ServerRequest): ServerResponse {
        val organizations = eventService.findByYear(CURRENT_EVENT).organizations
        val donationByOrgas = repository.findAllByYear(CURRENT_EVENT)
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
        val orgs = (organizations.map { it.toSponsorDto(req.language()) })
        val params = mapOf(
            // "organizations" to orgs,
            "organization0" to if (orgs.isNotEmpty()) orgs[0] else null,
            "organization1" to if (orgs.size > 1) orgs[1] else null,
            "organization2" to if (orgs.size > 2) orgs[2] else null,
            "organization3" to if (orgs.size > 3) orgs[3] else null,
            "organization4" to if (orgs.size > 4) orgs[4] else null,
            "donations" to donationByOrgas,
            "loadAt" to LocalTime.now(ZoneId.of(TIMEZONE)).format(frenchTalkTimeFormatter),
            TITLE to MixetteDashboard.title
        )

        return ok().renderAndAwait(MixetteDashboard.template, params)
    }

    suspend fun mixetteRealTime(req: ServerRequest): ServerResponse {
        val interval = Flux.interval(ofMillis(10000))
        return ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(
                BodyInserters.fromServerSentEvents(
                    interval
                        .onBackpressureBuffer()
                        .map {
                            runBlocking {
                                repository
                                    .findByYearAfterNow(CURRENT_EVENT)
                                    .collectList()
                                    .awaitSingle()
                                    .let {
                                        // Here we have the list of all donation. We need to compute the 10 betters donors
                                        // and the computation of all donations
                                        ServerSentEvent.builder<MixetteDashboardData>()
                                            .event("message")
                                            .data(computeBetterDonorAndAggregateResult(it))
                                            .build()
                                    }
                            }
                        }
                )
            )
            .awaitSingle()
    }

    private suspend fun computeBetterDonorAndAggregateResult(donations: List<MixetteDonation>): MixetteDashboardData {
        val tenBetterDonors: List<MixetteDashboardUser> = donations
            .groupBy { it.encryptedTicketNumber!! }
            .mapValues { entries -> entries.value.sumOf { it.quantity } }
            .toSortedMap()
            .asIterable()
            .map {
                val user =
                    ticketService.findByNumber(cryptographer.decrypt(it.key)!!) ?: throw IllegalArgumentException()
                MixetteDashboardUser(user, it.value)
            }
            .sortedByDescending { it.quantity }
            .take(10)

        val donationByOrgas: List<MixetteDashboardOrganisation> = donations
            .groupBy { it.organizationLogin }
            .mapValues { entries -> entries.value.sumOf { it.quantity } }
            .toSortedMap()
            .map {
                val user = userService.findOneOrNull(it.key) ?: throw IllegalArgumentException()
                MixetteDashboardOrganisation(user, it.value, properties)
            }

        return MixetteDashboardData(tenBetterDonors, donationByOrgas)
    }

    data class MixetteDashboardData(
        val ranking: List<MixetteDashboardUser>,
        val donations: List<MixetteDashboardOrganisation>
    )

    data class MixetteDashboardUser(
        val login: String?,
        val firstname: String?,
        val lastname: String,
        val quantity: Int
    ) {
        constructor(user: CachedTicket, quantity: Int) : this(
            user.login,
            user.firstname,
            user.lastname,
            quantity
        )
    }

    data class MixetteDashboardOrganisation(
        val login: String,
        val company: String?,
        val photoUrl: String?,
        val description: String?,
        val quantity: Int,
        val amount: Double,
    ) {
        constructor(user: CachedUser, quantity: Int, properties: MixitProperties) : this(
            user.login,
            user.company,
            user.photoUrl,
            user.description[Language.FRENCH],
            quantity,
            properties.mixetteValue.multiply(BigDecimal.valueOf(quantity.toLong())).toDouble()
        )
    }
}
