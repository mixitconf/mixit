package mixit.security.model

import mixit.MixitProperties
import mixit.ticket.repository.LotteryRepository
import mixit.user.model.*
import mixit.user.repository.UserRepository
import mixit.util.camelCase
import mixit.util.email.EmailService
import mixit.util.errors.*
import mixit.util.toSlug
import mixit.util.validator.EmailValidator
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val ticketRepository: LotteryRepository,
    private val emailService: EmailService,
    private val emailValidator: EmailValidator,
    private val cryptographer: Cryptographer,
    private val properties: MixitProperties
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Create user from HTTP form or from a ticket
     */
    fun createUser(nonEncryptedMail: String?, firstname: String?, lastname: String?): Pair<String, User> =
            emailValidator.check(nonEncryptedMail).let { email ->
                if (firstname == null || lastname == null) {
                    throw CredentialValidatorException()
                }
                Pair(
                        email,
                        User(
                                login = email.split("@")[0].toSlug(),
                                firstname = firstname.camelCase(),
                                lastname = lastname.camelCase(),
                                email = cryptographer.encrypt(email),
                                role = Role.USER
                        )
                )
            }

    /**
     * Create user if he does not exist
     */
    fun createUserIfEmailDoesNotExist(nonEncryptedMail: String, user: User): Mono<User> =
            userRepository.findOne(user.login)
                    .flatMap { Mono.error<User> { DuplicateException("Login already exist") } }
                    .switchIfEmpty(
                            Mono.defer {
                                userRepository.findByNonEncryptedEmail(nonEncryptedMail)
                                        // Email is unique and if an email is found we return an error
                                        .flatMap { Mono.error<User> { DuplicateException("Email already exist") } }
                                        .switchIfEmpty(Mono.defer { userRepository.save(user) })
                            }
                    )

    /**
     * This function try to find a user in the user table and if not try to read his information in
     * ticketing table to create a new one.
     */
    fun searchUserByEmailOrCreateHimFromTicket(nonEncryptedMail: String): Mono<User> =
            userRepository.findByNonEncryptedEmail(nonEncryptedMail)
                    .switchIfEmpty(
                            Mono.defer {
                                ticketRepository.findByEmail(nonEncryptedMail)
                                        .flatMap { createUserIfEmailDoesNotExist(nonEncryptedMail, createUser(nonEncryptedMail, it.firstname, it.lastname).second) }
                                        .switchIfEmpty(Mono.empty<User>())
                            }
                    )

    fun createCookie(user: User) = ResponseCookie
            .from("XSRF-TOKEN", user.jsonToken(cryptographer))
            .maxAge(user.tokenLifeTime)
            .build()

    /**
     * Function used on login to check if user email and token are valids
     */
    fun checkUserEmailAndToken(nonEncryptedMail: String, token: String): Mono<User> =
            userRepository.findByNonEncryptedEmail(nonEncryptedMail)
                    .flatMap {
                        if (it.hasValidToken(token.trim())) {
                            return@flatMap Mono.just(it)
                        }
                        return@flatMap Mono.error<User> { TokenException("Token is invalid or is expired") }
                    }
                    .switchIfEmpty(Mono.defer { throw NotFoundException() })

    /**
     * Function used on login to check if user email and token are valids
     */
    fun checkUserEmailAndTokenOrAppToken(nonEncryptedMail: String, token: String?, appToken: String?): Mono<User> =
            userRepository.findByNonEncryptedEmail(nonEncryptedMail)
                    .flatMap {
                        if (it.hasValidTokens(token, appToken)) {
                            return@flatMap Mono.just(it)
                        }
                        return@flatMap Mono.error<User> { TokenException("Token is invalid or is expired") }
                    }
                    .switchIfEmpty(Mono.defer { throw NotFoundException() })

    /**
     * Sends an email with a token to the user. We don't need validation of the email adress. If he receives
     * the email it's OK. If he retries a login a new token is sent. Be careful email service can throw
     * an EmailSenderException
     */
    fun generateAndSendToken(user: User, locale: Locale, generateExternalToken: Boolean = false): Mono<User> =
            user.generateNewToken(generateExternalToken).let { newUser ->
                try {
                    if (!properties.feature.email) {
                        logger.info("A token ${newUser.token} was sent by email")
                    }
                    emailService.send("email-token", newUser, locale)
                    userRepository.save(newUser)
                } catch (e: EmailSenderException) {
                    Mono.error<User> { e }
                }
            }

    /**
     * Sends an email with a token to the user. We don't need validation of the email adress.
     */
    fun clearToken(nonEncryptedMail: String): Mono<User> =
            userRepository
                    .findByNonEncryptedEmail(nonEncryptedMail)
                    .flatMap { user -> user.generateNewToken().let { userRepository.save(it) } }
}
