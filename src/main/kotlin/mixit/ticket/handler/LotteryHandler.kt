package mixit.ticket.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.features.model.Feature
import mixit.features.model.FeatureStateService
import mixit.routes.MustacheI18n.TITLE
import mixit.routes.MustacheTemplate
import mixit.routes.MustacheTemplate.LotteryClosed
import mixit.routes.MustacheTemplate.LotteryEdit
import mixit.routes.MustacheTemplate.LotteryError
import mixit.routes.MustacheTemplate.LotterySubmission
import mixit.security.model.Cryptographer
import mixit.ticket.model.LotteryTicket
import mixit.ticket.repository.LotteryRepository
import mixit.user.model.User
import mixit.util.camelCase
import mixit.util.email.EmailService
import mixit.util.extractFormData
import org.springframework.web.reactive.function.server.json
import mixit.util.locale
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.renderAndAwait
import java.util.*

@Component
class LotteryHandler(
    private val lotteryRepository: LotteryRepository,
    private val cryptographer: Cryptographer,
    private val emailService: EmailService,
    private val featureStateService: FeatureStateService
) {
    suspend fun findAll(req: ServerRequest): ServerResponse =
        ok().json().bodyValueAndAwait(lotteryRepository.findAll())

    suspend fun ticketing(req: ServerRequest): ServerResponse =
        ok().renderAndAwait(
            if (featureStateService.findOneByType(Feature.Lottery).active) LotteryEdit.template else LotteryClosed.template,
            mapOf(TITLE to LotteryEdit.title)
        )

    suspend fun submit(req: ServerRequest): ServerResponse {
        val formData = req.extractFormData()
        val ticket = LotteryTicket(
            formData["email"]!!.lowercase(),
            formData["firstname"]!!,
            formData["lastname"]!!,
            interests = formData["interests"]?.split(",")?.map { it.trim() } ?: emptyList(),
        )

        // Data are encrypted
        val lotteryTicket = lotteryRepository
            .save(
                ticket.copy(
                    email = cryptographer.encrypt(ticket.email)!!,
                    firstname = cryptographer.encrypt(ticket.firstname.camelCase())!!,
                    lastname = cryptographer.encrypt(ticket.lastname.camelCase())!!
                )
            ).awaitSingle()

        return try {
            sendUserConfirmation(lotteryTicket, formData, req.locale())
        } catch (e: DuplicateKeyException) {
            ok().renderAndAwait(
                LotteryError.template,
                mapOf(TITLE to "ticketing.title", "message" to "ticketing.error.alreadyexists")
            )
        } catch (e: Exception) {
            ok().renderAndAwait(
                LotteryError.template,
                mapOf(TITLE to "ticketing.title", "message" to "ticketing.error.default")
            )
        }
    }

    private suspend fun sendUserConfirmation(
        ticket: LotteryTicket,
        formData: Map<String, String?>,
        locale: Locale
    ): ServerResponse {
        val user = User(
            cryptographer.decrypt(ticket.email)!!,
            cryptographer.decrypt(ticket.firstname)!!,
            cryptographer.decrypt(ticket.lastname)!!,
            ticket.email
        )
        emailService.send(MustacheTemplate.EmailTicketing.template, user, locale)
        return ok().render(LotterySubmission.template, formData).awaitSingle()
    }
}
