package mixit.security.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.favorite.handler.FavoriteDto
import mixit.favorite.model.Favorite
import mixit.favorite.repository.FavoriteRepository
import mixit.security.handler.ExternalResponses.CREDENTIAL_VALID
import mixit.security.handler.ExternalResponses.EMAIL_SENT_ERROR
import mixit.security.handler.ExternalResponses.INVALID_CREDENTIALS
import mixit.security.handler.ExternalResponses.INVALID_EMAIL
import mixit.security.handler.ExternalResponses.INVALID_TOKEN
import mixit.security.handler.ExternalResponses.TOKEN_SENT
import mixit.security.model.AuthenticationService
import mixit.security.model.Cryptographer
import mixit.ticket.repository.LotteryRepository
import mixit.user.model.User
import mixit.util.errors.EmailValidatorException
import mixit.util.errors.NotFoundException
import mixit.util.errors.TokenException
import org.springframework.web.reactive.function.server.json
import mixit.util.locale
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.queryParamOrNull
import reactor.kotlin.core.publisher.toMono

@Component
class ExternalHandler(
    private val authenticationService: AuthenticationService,
    private val favoriteRepository: FavoriteRepository,
    private val ticketRepository: LotteryRepository,
    private val cryptographer: Cryptographer
) {

    /**
     * Some external request needs to be secured. So for them we need in the request the email and token
     */
    private suspend fun credentials(
        req: ServerRequest,
        strictMode: Boolean,
        action: suspend (nonEncryptedEmail: String, user: User) -> ServerResponse
    ): ServerResponse {
        // Email is always required
        val email = req.queryParamOrNull("email")
        // In strict mode we only check user token. In normal mode we check user token or external app token
        val token = req.queryParamOrNull("token")

        if (strictMode) {
            if (email == null || token == null) {
                return INVALID_CREDENTIALS.response()
            }
            try {
                return authenticationService.checkUserEmailAndToken(email, token).let { action.invoke(email, it) }
            } catch (e: Exception) {
                return when (e) {
                    is TokenException -> INVALID_TOKEN.response()
                    is NotFoundException -> INVALID_EMAIL.response()
                    else -> INVALID_CREDENTIALS.response()
                }
            }
        } else {
            val externalAppToken = req.queryParamOrNull("external-token")
            if (email == null || (token == null && externalAppToken == null)) {
                return INVALID_CREDENTIALS.response()
            }
            try {
                return authenticationService.checkUserEmailAndTokenOrAppToken(email, token, externalAppToken)
                    .let { action.invoke(email, it) }
            } catch (e: Exception) {
                return when (e) {
                    is TokenException -> INVALID_TOKEN.response()
                    is NotFoundException -> INVALID_EMAIL.response()
                    else -> INVALID_CREDENTIALS.response()
                }
            }
        }
    }

    suspend fun sendToken(req: ServerRequest): ServerResponse {
        val email = req.queryParamOrNull("email") ?: return INVALID_CREDENTIALS.response()
        try {
            val user =
                authenticationService.searchUserByEmailOrCreateHimFromTicket(email) ?: return INVALID_EMAIL.response()
            // If user is found we send him a token
            authenticationService.generateAndSendToken(
                user,
                req.locale(),
                email,
                generateExternalToken = true,
                tokenForNewsletter = false
            )
            // if token is sent we return a reponse to the caller
            return TOKEN_SENT.response()
        } catch (e: EmailValidatorException) {
            return INVALID_EMAIL.response()
        } catch (e: Exception) {
            // An error can occur when email is sent
            return EMAIL_SENT_ERROR.response()
        }
    }

    suspend fun checkToken(req: ServerRequest): ServerResponse =
        credentials(req, true) { _, _ -> CREDENTIAL_VALID.response() }

    suspend fun profile(req: ServerRequest): ServerResponse =
        credentials(req, false) { nonEncryptedEmail, user ->
            ticketRepository.findByEncryptedEmail(cryptographer.encrypt(nonEncryptedEmail)!!)
            ok().json().bodyValueAndAwait(ExternalUserDto(user))
        }

    suspend fun favorites(req: ServerRequest): ServerResponse =
        credentials(req, false) { nonEncryptedEmail, _ ->
            ok().json().bodyValueAndAwait(
                favoriteRepository.findByEmail(nonEncryptedEmail).map { FavoriteDto(it.talkId, true) }
            )
        }

    suspend fun favorite(req: ServerRequest): ServerResponse =
        credentials(req, false) { nonEncryptedEmail, _ ->
            ok().json().bodyValueAndAwait(
                favoriteRepository.findByEmailAndTalk(nonEncryptedEmail, req.pathVariable("id"))
                    ?.let { FavoriteDto(it.talkId, true).toMono() }
                    ?: FavoriteDto(req.pathVariable("id"), false)
            )
        }

    suspend fun toggleFavorite(req: ServerRequest): ServerResponse =
        credentials(req, false) { nonEncryptedEmail, user ->
            val favorite = favoriteRepository.findByEmailAndTalk(nonEncryptedEmail, req.pathVariable("id"))
            return@credentials if (favorite == null) {
                // we create it
                favoriteRepository.save(Favorite(user.email!!, req.pathVariable("id"))).awaitSingle()
                ok().json().bodyValueAndAwait(FavoriteDto(req.pathVariable("id"), true))
            } else {
                // if favorite is found we delete it
                favoriteRepository
                    .delete(nonEncryptedEmail, favorite.talkId).awaitSingle()
                ok().json().bodyValueAndAwait(FavoriteDto(favorite.talkId, false))
            }
        }
}
