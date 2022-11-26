package mixit.user.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.talk.model.TalkService
import mixit.user.handler.dto.toDto
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.anonymize
import mixit.user.repository.UserRepository
import mixit.util.errors.NotFoundException
import mixit.util.json
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.net.URI.create

@Component
class UserJsonHandler(
    private val repository: UserRepository,
    private val userService: UserService,
    private val service: TalkService
) {
    suspend fun findOne(req: ServerRequest): ServerResponse =
        repository.coFindOneOrNull(req.pathVariable("login"))
            ?.let { ok().json().bodyValueAndAwait(it.anonymize()) }
            ?: throw NotFoundException()

    suspend fun findAll(req: ServerRequest): ServerResponse =
        repository
            .coFindAll()
            .map { it.anonymize() }
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun findStaff(req: ServerRequest): ServerResponse =
        repository
            .coFindByRoles(listOf(Role.STAFF))
            .map { it.anonymize() }
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun findOneStaff(req: ServerRequest): ServerResponse =
        repository
            .coFindOneByRoles(req.pathVariable("login"), listOf(Role.STAFF, Role.STAFF_IN_PAUSE))
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun findSpeakerByEventId(req: ServerRequest): ServerResponse =
        service
            .coFindByEvent(req.pathVariable("year"))
            .map { talk -> talk.speakers.map { it.anonymize() }.distinct() }
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun create(req: ServerRequest): ServerResponse =
        req.bodyToMono<User>()
            .flatMap { userService.save(it) }
            .flatMap { created(create("/api/user/${it.login}")).json().bodyValue(it) }
            .awaitSingle()

    suspend fun check(req: ServerRequest): ServerResponse =
        repository.coFindByNonEncryptedEmail(req.pathVariable("email"))
            .takeIf { it.token == req.headers().header("token")[0] }?.toDto(req.language())
            ?.let { ok().json().bodyValueAndAwait(it) }
            ?: throw NotFoundException()
}
