package mixit.security.model

import kotlinx.coroutines.reactor.awaitSingle
import mixit.MixitProperties
import mixit.routes.MustacheTemplate
import mixit.security.MixitWebFilter.Companion.AUTHENT_COOKIE
import mixit.ticket.repository.LotteryRepository
import mixit.user.model.Role
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.model.generateNewToken
import mixit.user.model.hasValidToken
import mixit.user.model.hasValidTokens
import mixit.user.model.jsonToken
import mixit.user.repository.UserRepository
import mixit.util.camelCase
import mixit.util.email.EmailService
import mixit.util.errors.CredentialValidatorException
import mixit.util.errors.DuplicateException
import mixit.util.errors.NotFoundException
import mixit.util.errors.TokenException
import mixit.util.toSlug
import mixit.util.validator.EmailValidator
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val lotteryRepository: LotteryRepository,
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
    suspend fun createUserIfEmailDoesNotExist(nonEncryptedMail: String, user: User): User {
        userRepository.findOneOrNull(user.login)
            ?.also {
                throw DuplicateException("Login already exist")
            }
        // Email is unique and if an email is found we return an error
        userRepository.findByNonEncryptedEmail(nonEncryptedMail)
            ?.also {
                throw DuplicateException("Email already exist")
            }
        return userRepository.save(user).awaitSingle()
    }

    /**
     * This function try to find a user in the user table and if not try to read his information in
     * ticketing table to create a new one.
     */
    suspend fun searchUserByEmailOrCreateHimFromTicket(nonEncryptedMail: String): User? =
        userRepository.findByNonEncryptedEmail(nonEncryptedMail)
        // If user is not found we search in the lottery
            ?: lotteryRepository
                .findByEncryptedEmail(cryptographer.encrypt(nonEncryptedMail)!!)
                ?.let {
                    createUserIfEmailDoesNotExist(
                        nonEncryptedMail,
                        createUser(nonEncryptedMail, it.firstname, it.lastname).second
                    )
                }

    fun createCookie(user: User) = ResponseCookie
        .from(AUTHENT_COOKIE, user.jsonToken(cryptographer))
        .secure(properties.baseUri.startsWith("https"))
        .httpOnly(true)
        .maxAge(user.tokenLifeTime)
        .build()

    /**
     * Function used on login to check if user email and token are valids
     */
    suspend fun checkUserEmailAndToken(nonEncryptedMail: String, token: String): User =
        userRepository.findByNonEncryptedEmail(nonEncryptedMail)
            ?.let {
                if (it.hasValidToken(token.trim())) {
                    return it
                }
                throw TokenException("Token is invalid or is expired")
            }
            ?: throw NotFoundException()

    /**
     * Function used on login to check if user email and token are valids
     */
    suspend fun checkUserEmailAndTokenOrAppToken(nonEncryptedMail: String, token: String?, appToken: String?): User =
        userRepository.findByNonEncryptedEmail(nonEncryptedMail)
            ?.let {
                if (it.hasValidTokens(token, appToken)) {
                    return it
                }
                throw TokenException("Token is invalid or is expired")
            }
            ?: throw NotFoundException()

    /**
     * Sends an email with a token to the user. We don't need validation of the email adress. If he receives
     * the email it's OK. If he retries a login a new token is sent. Be careful email service can throw
     * an EmailSenderException
     */
    suspend fun generateAndSendToken(
        user: User,
        locale: Locale,
        nonEncryptedMail: String,
        tokenForNewsletter: Boolean,
        generateExternalToken: Boolean = false
    ): User {
        val newUser = user.generateNewToken(generateExternalToken)
        if (!properties.feature.email) {
            logger.info("A token ${newUser.token} was sent by email")
        }
        emailService.send(
            if (tokenForNewsletter) MustacheTemplate.EmailNewsletterSubscribe.template else MustacheTemplate.EmailToken.template,
            newUser,
            locale
        )
        return userRepository.save(newUser).awaitSingle()
    }

    /**
     * Sends an email with a token to the user. We don't need validation of the email adress.
     */
    suspend fun clearToken(nonEncryptedMail: String): User =
        userRepository
            .findByNonEncryptedEmail(nonEncryptedMail)
            ?.generateNewToken()
            ?.let {
                userRepository.save(it).awaitSingle()
            }
            ?: throw NotFoundException()

    suspend fun updateNewsletterSubscription(user: User, tokenForNewsletter: Boolean): User =
        if (tokenForNewsletter) {
            userRepository.save(user.copy(newsletterSubscriber = true)).awaitSingle()
        } else if (user.email == null) {
            // Sometimes we can have a email hash in the DB but not the email (for legacy users). So in this case
            // we store the email
            userRepository.save(userService.updateReference(user) ?: throw NotFoundException()).awaitSingle()
        } else {
            user
        }
}
