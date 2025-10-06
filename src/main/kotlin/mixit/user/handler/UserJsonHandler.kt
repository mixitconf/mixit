package mixit.user.handler

import kotlinx.coroutines.reactor.awaitSingle
import mixit.security.model.Cryptographer
import mixit.talk.model.TalkService
import mixit.user.handler.dto.toDto
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.anonymize
import mixit.user.repository.UserRepository
import mixit.util.errors.NotFoundException
import mixit.util.language
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import java.net.URI.create

@Component
class UserJsonHandler(
    private val repository: UserRepository,
    private val userService: UserService,
    private val service: TalkService,
    private val cryptographer: Cryptographer
) {
    companion object {
        val EXCLUDED = listOf(
            "@mxit.im",
            "9576422474@mixitconf.org",
            "TODO@TODO.com",
            "aname6489@gmail.com",
            " aniketdebnath640@gmail.com",
            "anishshaikh5478@gmail.com",
            " anishsivani254@gmail.com",
            "anjelicia.lobai03@gmail.com",
            "ankeshambat059@gmail.com",
            "ankeshkumarsinha13@gmail.com",
            "arbajali6229@gmail.com",
            "ashrafulalam01761022924@gmail.com",
            "ashrobali188@gmail.com",
            "asiaku81@gmail.com",
            "ayandasajini8@gmail.com",
            "ayudelana1@gmail.com",
            "azertypow@gmail.com",
            "bgmib701@gamil.com",
            "biswasrohit8777@gmail.com",
            "bophelosejake@gmail.com",
            "bormonayon1@gmail.com",
            "bradleymanewil131@gmail.comthe",
            " breytenbachant@gmail.com",
            "burdakvishnu388@gmail.com",
            "cameron@gmail.com",
            "chirsdube4422@gmail.com",
            "camille.fake.tester@mixitconf.org",
            "dre336699@gmail.com",
            "duanedozafelix@gmail.com",
            "foo-bar@example.com",
            "foreveryoung@mxitconf.org",
            "hhuhu@mox.com",
            "hjpavithraram@gmail.com",
            "ivan@culjak.xyz",
            "ks7544094@gmail.com",
            "kudakwasheshinga@yahoo.com",
            "laurabinda15@gmail.com",
            "msizisiwisa@gmail.com",
            "mzwandilec58@outlook.com",
            "nazirshaheena1@gmail.com",
            "ncamiehkhumalo44@gmai.com",
            "neljosef35@gmail.com"
        )
    }

    suspend fun findOne(req: ServerRequest): ServerResponse =
        repository.findOneOrNull(req.pathVariable("login"))
            ?.let { ok().json().bodyValueAndAwait(it.anonymize(cryptographer)) }
            ?: throw NotFoundException()

    suspend fun findAll(req: ServerRequest): ServerResponse =
        repository
            .findAll()
            .filterNot { user ->
                EXCLUDED.any {
                    user.email?.lowercase()?.endsWith(it.lowercase())
                        ?: cryptographer.decrypt(user.email)?.lowercase()?.endsWith(it.lowercase())
                    ?: false
                }

            }
            .map { it.anonymize(cryptographer) }
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun findSpeakerByEventId(req: ServerRequest): ServerResponse =
        service
            .findByEvent(req.pathVariable("year"))
            .flatMap { talk -> talk.speakers.map { it.anonymize(null) }.distinct() }
            .let { ok().json().bodyValueAndAwait(it) }

    suspend fun create(req: ServerRequest): ServerResponse =
        req.bodyToMono<User>()
            .flatMap { userService.save(it) }
            .flatMap { created(create("/api/user/${it.login}")).json().bodyValue(it) }
            .awaitSingle()

    suspend fun check(req: ServerRequest): ServerResponse =
        repository.findByNonEncryptedEmail(req.pathVariable("email"))
            ?.takeIf { it.token == req.headers().header("token")[0] }?.toDto(req.language())
            ?.let { ok().json().bodyValueAndAwait(it) }
            ?: throw NotFoundException()
}
