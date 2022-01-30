package mixit.security.handler

import mixit.util.json
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

enum class ExternalResponses(val status: HttpStatus, val message: String) {
    CREDENTIAL_VALID(OK, "Credentials are valids"),
    TOKEN_SENT(OK, "A token was send by email. Please check your mailbox and send it in the future request"),
    INVALID_TOKEN(BAD_REQUEST, "Token is invalid"),
    INVALID_CREDENTIALS(BAD_REQUEST, "Credentials are invalids"),
    INVALID_EMAIL(NOT_FOUND, "Account not found"),
    EMAIL_SENT_ERROR(INTERNAL_SERVER_ERROR, "An unexpected error occurred on email sent");

    fun response(): Mono<ServerResponse> = ServerResponse.status(this.status).json().bodyValue(this.message)
}
