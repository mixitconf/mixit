package mixit.web.service

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import mixit.model.Role
import mixit.model.Ticket
import mixit.model.User
import mixit.repository.TicketRepository
import mixit.repository.UserRepository
import mixit.util.Cryptographer
import mixit.util.EmailService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockKExtension::class)
internal class AuthenticationServiceTest {

    @MockK
    lateinit var userRepository: UserRepository
    @MockK
    lateinit var ticketRepository: TicketRepository
    @MockK
    lateinit var emailService: EmailService
    @MockK
    lateinit var cryptographer: Cryptographer

    @InjectMockKs
    lateinit var service: AuthenticationService

    val anEmail = "dan@north.uk"
    val aUser = User("tastapod", "Dan", "North", anEmail, role = Role.USER, token = "token", tokenExpiration = LocalDateTime.now().plusDays(1))

    @Test
    fun `should create nothing  if a user already use this email`() {
        every { userRepository.findOne(any()) } returns Mono.empty()
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser)

        StepVerifier.create(service.createUserIfEmailDoesNotExist(anEmail, aUser))
                .consumeErrorWith { DuplicateException("Email already exist") }
                .verify()
    }

    @Test
    fun `should create nothing if a user already use this login`() {
        every { userRepository.findOne(any()) } returns Mono.just(aUser)

        StepVerifier.create(service.createUserIfEmailDoesNotExist(anEmail, aUser))
                .consumeErrorWith { DuplicateException("Login already exist") }
                .verify()
    }

    @Test
    fun `should create a new user if no user use this email`() {
        every { userRepository.findOne(any()) } returns Mono.empty()
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.empty()
        every { userRepository.save(aUser) } returns Mono.just(aUser)

        StepVerifier.create(service.createUserIfEmailDoesNotExist(anEmail, aUser))
                .expectNext(aUser)
                .verifyComplete()
    }

    @Test
    fun `should search and find user with his email`() {
        every { userRepository.findByNonEncryptedEmail(anEmail) } returns Mono.just(aUser)

        StepVerifier.create(service.searchUserByEmailOrCreateHimFromTicket(anEmail))
                .expectNext(aUser)
                .verifyComplete()
    }

    @Test
    fun `should search and create user from ticket if we can't find him with his email`() {
        // User does not exist
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.empty()
        // we find him in tickets
        every { ticketRepository.findByEmail(any()) } returns Mono.just(Ticket(anEmail, "Dan", "North"))
        // the cryptographer is used when the user with an encrypted email is created
        every { cryptographer.encrypt(any()) } returns anEmail
        // No user use this login
        every { userRepository.findOne(any()) } returns Mono.empty()
        // the new user is saved
        every { userRepository.save(any() as User) } returns Mono.just(aUser)

        StepVerifier.create(service.searchUserByEmailOrCreateHimFromTicket(anEmail))
                .expectNext(aUser)
                .verifyComplete()

    }

    @Test
    fun `should search and return empty if user is nor in user database nor in ticket database`() {
        // User does not exist
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.empty()
        // we find him in tickets
        every { ticketRepository.findByEmail(any()) } returns Mono.empty()

        // Noting is expected
        StepVerifier.create(service.searchUserByEmailOrCreateHimFromTicket(anEmail)).verifyComplete()

    }

    @Test
    fun `should check user email and user token when everything is OK`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser)

        StepVerifier.create(service.checkUserEmailAndToken(anEmail, aUser.token))
                .expectNext(aUser)
                .verifyComplete()
    }

    @Test
    fun `should check user email and user token and return token exception when token is expired`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser.copy(tokenExpiration = LocalDateTime.now().minusMinutes(30)))

        StepVerifier.create(service.checkUserEmailAndToken(anEmail, aUser.token))
                .consumeErrorWith { TokenException("Token is invalid or is expired") }
                .verify()
    }

    @Test
    fun `should check user email and user token and return token exception when token is not found`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.just(aUser)

        StepVerifier.create(service.checkUserEmailAndToken(anEmail, "invalid token"))
                .consumeErrorWith { TokenException("Token is invalid or is expired") }
                .verify()
    }

    @Test
    fun `should check user email and user token and return notfound exception when user is not found`() {
        every { userRepository.findByNonEncryptedEmail(any()) } returns Mono.empty()

        StepVerifier.create(service.checkUserEmailAndToken("invalid email", "invalid token"))
                .consumeErrorWith { NotFoundException() }
                .verify()
    }

    @Test
    fun `should generate and save new user token but not send it`() {
        every { userRepository.save(any() as User) } returns Mono.just(aUser)

        StepVerifier.create(service.generateAndSendToken(aUser, Locale.FRANCE, false))
                .expectNext(aUser)
                .verifyComplete()

        // We check that email service was not called
        confirmVerified(emailService)
    }

    @Test
    fun `should generate, save and send new user token`() {
        every { userRepository.save(any() as User) } returns Mono.just(aUser)
        every { emailService.send(any(), any(), any()) } answers {}

        StepVerifier.create(service.generateAndSendToken(aUser, Locale.FRANCE, true))
                .expectNext(aUser)
                .verifyComplete()

        verify { emailService.send(any(), any(), any()) }
        confirmVerified(emailService)
    }

    @Test
    fun `should generate but not save new user token if exception during mail sent`() {
        every { userRepository.save(any() as User) } returns Mono.just(aUser)
        every { emailService.send(any(), any(), any()) } throws EmailSenderException("Error on mail sent")

        StepVerifier.create(service.generateAndSendToken(aUser, Locale.FRANCE, true))
                .consumeErrorWith { EmailSenderException("Error on mail sent") }
                .verify()

        verify { emailService.send(any(), any(), any()) }
        confirmVerified(emailService)
    }
}