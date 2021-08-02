package mixit.web.handler

import mixit.model.*
import mixit.repository.FavoriteRepository
import mixit.repository.TicketRepository
import mixit.util.json
import mixit.util.locale
import mixit.web.handler.ExternalHandler.MixiTResponses.*
import mixit.web.service.AuthenticationService
import mixit.web.service.EmailValidatorException
import mixit.web.service.NotFoundException
import mixit.web.service.TokenException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono


@Component
class ExternalHandler(private val authenticationService: AuthenticationService,
                      private val favoriteRepository: FavoriteRepository,
                      private val ticketRepository: TicketRepository) {

    private data class MixiTResponse(val status: Int, val message: String)

    private enum class MixiTResponses(val message: MixiTResponse, val status: HttpStatus) {
        CREDENTIAL_VALID(MixiTResponse(OK.value(), "Credentials are valids"), OK),
        TOKEN_SENT(MixiTResponse(OK.value(), "A token was send by email. Please check your mailbox and send it in the future request"), OK),
        INVALID_TOKEN(MixiTResponse(BAD_REQUEST.value(), "Token is invalid"), BAD_REQUEST),
        INVALID_EMAIL(MixiTResponse(NOT_FOUND.value(), "Account not found"), NOT_FOUND),
        INVALID_CREDENTIALS(MixiTResponse(BAD_REQUEST.value(),"Credentials are invalid"), BAD_REQUEST),
        EMAIL_NOT_KNOWN(MixiTResponse(BAD_REQUEST.value(),"This email is not known. You have to create an account on our website if you want to use this functionnality"), BAD_REQUEST),
        EMAIL_SENT_ERROR(MixiTResponse(INTERNAL_SERVER_ERROR.value(),"An expected error occured on email sent"), INTERNAL_SERVER_ERROR);

        fun response(): Mono<ServerResponse> = status(this.status).json().bodyValue(this.message)
    }

    /**
     * Some external request needs to be secured. So for them we need in the request the email and token
     */
    private fun credentials(req: ServerRequest, action: (nonEncryptedEmail: String, user: User) -> Mono<ServerResponse>): Mono<ServerResponse> {
        val email = req.queryParamOrNull("email")
        val token = req.queryParamOrNull("token")
        if (email == null || token == null) {
            return INVALID_CREDENTIALS.response()
        }
        return authenticationService.checkUserEmailAndToken(email, token)
                .flatMap { action.invoke(email, it) }
                .onErrorResume {
                    when(it) {
                        is TokenException -> INVALID_TOKEN.response()
                        is NotFoundException -> INVALID_EMAIL.response()
                        else ->  INVALID_CREDENTIALS.response()
                    }
                }
    }

    fun sendToken(req: ServerRequest): Mono<ServerResponse> =
        req.queryParamOrNull("email").let {
            if(it == null){
                INVALID_CREDENTIALS.response()
            }
            else {
                try {
                    authenticationService.searchUserByEmailOrCreateHimFromTicket(it)
                            .flatMap {
                                // If user is found we send him a token
                                authenticationService.generateAndSendToken(it, req.locale())
                                        // if token is sent we return a reponse to the caller
                                        .flatMap { TOKEN_SENT.response() }
                                        // An error can occur when email is sent
                                        .onErrorResume { EMAIL_SENT_ERROR.response() }
                            }
                            // An error can be thrown when we try to create a user from ticketting
                            .onErrorResume { INVALID_EMAIL.response() }
                            // if user is not found we ask him to use website
                            .switchIfEmpty(Mono.defer { EMAIL_NOT_KNOWN.response() })
                } catch (e: EmailValidatorException) {
                    INVALID_EMAIL.response()
                }
            }
        }


    fun checkToken(req: ServerRequest): Mono<ServerResponse> = credentials(req) { _, _ -> CREDENTIAL_VALID.response() }

    fun profile(req: ServerRequest): Mono<ServerResponse> = credentials(req) { nonEncryptedEmail, user ->
        ticketRepository
                .findByEmail(nonEncryptedEmail)
                .flatMap {
                    ok().json().bodyValue(ExternalUserDto(user, true))
                }
                .switchIfEmpty(
                        Mono.defer { ok().json().bodyValue(ExternalUserDto(user, false)) }
                )
    }

    fun favorites(req: ServerRequest): Mono<ServerResponse> = credentials(req) { nonEncryptedEmail, _ ->
        ok().json().body(favoriteRepository.findByEmail(nonEncryptedEmail).map { FavoriteDto(it.talkId, true) })
    }

    fun favorite(req: ServerRequest): Mono<ServerResponse> = credentials(req) { nonEncryptedEmail, _ ->
        ok().json().body(favoriteRepository.findByEmailAndTalk(nonEncryptedEmail, req.pathVariable("id"))
                .flatMap { FavoriteDto(it.talkId, true).toMono() }
                .switchIfEmpty(FavoriteDto(req.pathVariable("id"), false).toMono()))
    }

    fun toggleFavorite(req: ServerRequest): Mono<ServerResponse> = credentials(req) { nonEncryptedEmail, user ->
        favoriteRepository.findByEmailAndTalk(nonEncryptedEmail, req.pathVariable("id"))
                // if favorite is found we delete it
                .flatMap { favorite ->
                    favoriteRepository
                            .delete(nonEncryptedEmail, favorite.talkId)
                            .flatMap { ok().json().bodyValue(FavoriteDto(favorite.talkId, false)) }
                }
                // otherwise we create it
                .switchIfEmpty(Mono.defer {
                    favoriteRepository
                            .save(Favorite(user.email!!, req.pathVariable("id")))
                            .flatMap { ok().json().bodyValue(FavoriteDto(req.pathVariable("id"), true)) }
                })
    }

}

class ExternalUserDto(
        val login: String,
        val firstname: String,
        val lastname: String,
        val links: List<Link> = listOf(),
        val description: Map<Language, String> = emptyMap(),
        val hasLotteryTicket: Boolean = false,
        val photo: String? = null,
        val company: String? = null
) {
    constructor(user: User, hasLotteryTicket: Boolean = false) : this(
            user.login,
            user.firstname,
            user.lastname,
            user.links,
            user.description,
            hasLotteryTicket,
            if (user.photoUrl != null) user.photoUrl else Users.DEFAULT_IMG_URL,
            user.company)
}

