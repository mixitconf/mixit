package mixit.security.handler

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
import mixit.ticket.repository.LotteryRepository
import mixit.user.model.User
import mixit.util.errors.EmailValidatorException
import mixit.util.errors.NotFoundException
import mixit.util.errors.TokenException
import mixit.util.json
import mixit.util.locale
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.queryParamOrNull
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class ExternalHandler(
    private val authenticationService: AuthenticationService,
    private val favoriteRepository: FavoriteRepository,
    private val ticketRepository: LotteryRepository
) {

    /**
     * Some external request needs to be secured. So for them we need in the request the email and token
     */
    private fun credentials(
        req: ServerRequest,
        strictMode: Boolean,
        action: (nonEncryptedEmail: String, user: User) -> Mono<ServerResponse>
    ): Mono<ServerResponse> {
        // Email is always required
        val email = req.queryParamOrNull("email")
        // In strict mode we only check user token. In normal mode we check user token or external app token
        val token = req.queryParamOrNull("token")

        if (strictMode) {
            if (email == null || token == null) {
                return INVALID_CREDENTIALS.response()
            }
            return authenticationService.checkUserEmailAndToken(email, token)
                .flatMap { action.invoke(email, it) }
                .onErrorResume {
                    when (it) {
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
            return authenticationService.checkUserEmailAndTokenOrAppToken(email, token, externalAppToken)
                .flatMap { action.invoke(email, it) }
                .onErrorResume {
                    when (it) {
                        is TokenException -> INVALID_TOKEN.response()
                        is NotFoundException -> INVALID_EMAIL.response()
                        else -> INVALID_CREDENTIALS.response()
                    }
                }
        }
    }

    fun sendToken(req: ServerRequest): Mono<ServerResponse> =
        req.queryParamOrNull("email").let {
            if (it == null) {
                return@let INVALID_CREDENTIALS.response()
            }
            try {
                authenticationService.searchUserByEmailOrCreateHimFromTicket(it)
                    .flatMap { user ->
                        // If user is found we send him a token
                        authenticationService.generateAndSendToken(user, req.locale(), it, generateExternalToken = true)
                            // if token is sent we return a reponse to the caller
                            .flatMap { TOKEN_SENT.response() }
                            // An error can occur when email is sent
                            .onErrorResume { EMAIL_SENT_ERROR.response() }
                    }
                    // An error can be thrown when we try to create a user from ticketting
                    .onErrorResume { INVALID_EMAIL.response() }
                    // if user is not found we ask him to use website
                    .switchIfEmpty(Mono.defer { INVALID_EMAIL.response() })
            } catch (e: EmailValidatorException) {
                INVALID_EMAIL.response()
            }
        }

    fun checkToken(req: ServerRequest): Mono<ServerResponse> =
        credentials(req, true) { _, _ -> CREDENTIAL_VALID.response() }

    fun profile(req: ServerRequest): Mono<ServerResponse> =
        credentials(req, false) { nonEncryptedEmail, user ->

            ticketRepository
                .findByEmail(nonEncryptedEmail)
                .flatMap {
                    ok().json().bodyValue(ExternalUserDto(user))
                }
                .switchIfEmpty(
                    Mono.defer { ok().json().bodyValue(ExternalUserDto(user)) }
                )
        }

    fun favorites(req: ServerRequest): Mono<ServerResponse> =
        credentials(req, false) { nonEncryptedEmail, _ ->
            ok().json().body(
                favoriteRepository.findByEmail(nonEncryptedEmail).map { FavoriteDto(it.talkId, true) }
            )
        }

    fun favorite(req: ServerRequest): Mono<ServerResponse> =
        credentials(req, false) { nonEncryptedEmail, _ ->
            ok().json().body(
                favoriteRepository.findByEmailAndTalk(nonEncryptedEmail, req.pathVariable("id"))
                    .flatMap { FavoriteDto(it.talkId, true).toMono() }
                    .switchIfEmpty(FavoriteDto(req.pathVariable("id"), false).toMono())
            )
        }

    fun toggleFavorite(req: ServerRequest): Mono<ServerResponse> =
        credentials(req, false) { nonEncryptedEmail, user ->
            favoriteRepository.findByEmailAndTalk(nonEncryptedEmail, req.pathVariable("id"))
                // if favorite is found we delete it
                .flatMap { favorite ->
                    favoriteRepository
                        .delete(nonEncryptedEmail, favorite.talkId)
                        .flatMap { ok().json().bodyValue(FavoriteDto(favorite.talkId, false)) }
                }
                // otherwise we create it
                .switchIfEmpty(
                    Mono.defer {
                        favoriteRepository
                            .save(Favorite(user.email!!, req.pathVariable("id")))
                            .flatMap { ok().json().bodyValue(FavoriteDto(req.pathVariable("id"), true)) }
                    }
                )
        }
}
