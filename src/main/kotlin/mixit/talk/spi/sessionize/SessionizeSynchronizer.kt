package mixit.talk.spi.sessionize

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.reactor.awaitSingleOrNull
import mixit.MixitApplication
import mixit.MixitApplication.Companion.CURRENT_EVENT
import mixit.MixitApplication.Companion.TIMEZONE
import mixit.security.model.Cryptographer
import mixit.talk.model.Language
import mixit.talk.model.Room
import mixit.talk.model.Talk
import mixit.talk.model.TalkFormat
import mixit.talk.model.TalkService
import mixit.talk.model.Topic
import mixit.talk.repository.TalkRepository
import mixit.talk.spi.CfpSynchronizer
import mixit.talk.spi.sessionize.dto.SessionizeResponse
import mixit.talk.spi.sessionize.dto.SessionizeSpeaker
import mixit.user.model.Link
import mixit.user.model.User
import mixit.user.model.UserService
import mixit.user.repository.UserRepository
import mixit.util.camelCase
import mixit.util.encodeToMd5
import mixit.util.validator.MarkdownValidator
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

data class SpeakerMail(val email: String, val id: String)

@Service
class SessionizeSynchronizer(
    @Sessionize private val webClient: WebClient,
    private val userService: UserService,
    private val talkService: TalkService,
    private val objectMapper: ObjectMapper,
    private val cryptographer: Cryptographer,
    private val markdownValidator: MarkdownValidator,
    private val userRepository: UserRepository,
    private val talkRepository: TalkRepository
) : CfpSynchronizer {
    override suspend fun synchronize() {
        val response = webClient.get()
            .retrieve()
            .bodyToMono(SessionizeResponse::class.java)
            .awaitSingleOrNull()

        // Save speakers
        val speakers = synchronizeSpeakers(response?.speakers ?: emptyList())
        speakers.forEach { userRepository.save(it).awaitSingleOrNull() }
        userService.invalidateCache()

        // Save talks
        val formats = response?.formats() ?: emptyMap()
        val topics = response?.topics() ?: emptyMap()
        val languages = response?.languages() ?: emptyMap()
        val rooms = response?.rooms() ?: emptyMap()
        val levels = response?.levels() ?: emptyMap()

        val talks = (response?.sessions ?: emptyList())
            .map { session ->
                val talSpeakers = session.speakers.mapNotNull { speaker ->
                    speakers.find { it.cfpId == speaker }
                }
                val room = session.roomId?.let { rooms[it] } ?: Room.TWITCH
                val format = if(room == Room.TWITCH) TalkFormat.ON_AIR else session.categoryItems.firstNotNullOfOrNull { formats[it] } ?: TalkFormat.TALK
                val topic = session.categoryItems.firstNotNullOfOrNull { topics[it] } ?: Topic.OTHER
                val language = session.categoryItems.firstNotNullOfOrNull { languages[it] } ?: Language.FRENCH
                val level = session.categoryItems.firstNotNullOfOrNull { levels[it] }

                Talk(
                    id = session.id,
                    event = MixitApplication.CURRENT_EVENT,
                    format = format,
                    title = markdownValidator.sanitize(session.title),
                    summary = markdownValidator.sanitize(session.description),
                    description = null,
                    topic = topic.value,
                    language = language,
                    speakerIds = talSpeakers.map { it.login },
                    room = room,
                    addedAt = LocalDateTime.now(),
                    start = session.startsAt?.let { Instant.parse(it).atZone(ZoneId.of(TIMEZONE)).toLocalDateTime() },
                    end = session.endsAt?.let { Instant.parse(it).atZone(ZoneId.of(TIMEZONE)).toLocalDateTime() },
                    level = level
                )
            }
        talks.forEach { talkRepository.save(it).awaitSingleOrNull() }

        val talskIds = talks.map { it.id }
        val talksToDelete =
            talkRepository.findAll().filter { it.event == CURRENT_EVENT }.filterNot { talskIds.contains(it.id) }
        talksToDelete.onEach { talkRepository.deleteOne(it.id!!).awaitSingleOrNull() }

        talkService.invalidateCache()
    }

    suspend fun synchronizeSpeakers(speakers: List<SessionizeSpeaker>): List<User> {
        // We read the mapping between speaker id and email (exported manually from Sessionize)
        val emails = ClassPathResource("data/speakers_2024.json").inputStream.use { resource ->
            val speakersWithEmails: List<SpeakerMail> = objectMapper.readValue(resource)
            speakersWithEmails.map { it.copy(email = cryptographer.decrypt(it.email)!!) }
        }
        // We complete the speaker list with emails
        val speakersWithEmails = speakers.map {
            val email = emails.find { email -> email.id == it.id }
            it.copy(email = email?.email)
        }
        // Try to find user by email
        val existingSpeakers: List<User> = speakersWithEmails.mapNotNull { sessionizeSpeaker ->
            userService
                .findAll()
                .firstOrNull {
                    (sessionizeSpeaker.email != null && it.email == sessionizeSpeaker.email) ||
                            (it.cfpId == sessionizeSpeaker.id) ||
                            (it.firstname.lowercase() == sessionizeSpeaker.firstName.lowercase() &&
                                    it.lastname.lowercase() == sessionizeSpeaker.lastName.lowercase())
                }
                ?.let { user ->
                    user.copy(
                        firstname = sessionizeSpeaker.firstName.camelCase(),
                        lastname = sessionizeSpeaker.lastName.camelCase(),
                        email = sessionizeSpeaker.email?.let { cryptographer.encrypt(it) } ?: user.email,
                        description = mapOf(
                            Language.ENGLISH to markdownValidator.sanitize(sessionizeSpeaker.bio),
                            Language.FRENCH to markdownValidator.sanitize(sessionizeSpeaker.bio)
                        ),
                        links = sessionizeSpeaker.links.map { link ->
                            Link(link.title, link.url)
                        },
                        photoUrl = sessionizeSpeaker.picture,
                        emailHash = sessionizeSpeaker.email?.encodeToMd5(),
                        company = sessionizeSpeaker.tagLine,
                        cfpId = sessionizeSpeaker.id
                    ).toUser()
                }
        }
        val nonExistingSpeakers: List<User> = speakersWithEmails
            .filter { sessionizeSpeaker ->
                existingSpeakers.none { it.cfpId == sessionizeSpeaker.id }
            }
            .map { sessionizeSpeaker ->
                val login = sessionizeSpeaker.email?.let { it.substring(0, it.indexOf("@")) } ?: sessionizeSpeaker.id
                val userCheck = userRepository.findOneOrNull(login)
                if (userCheck != null) {
                    throw IllegalArgumentException("Login ${userCheck.login} already exist: try to create one with suffix")
                }
                User(
                    login = login,
                    firstname = sessionizeSpeaker.firstName.camelCase(),
                    lastname = sessionizeSpeaker.lastName.camelCase(),
                    email = sessionizeSpeaker.email?.let { cryptographer.encrypt(it) },
                    description = mapOf(
                        Language.ENGLISH to markdownValidator.sanitize(sessionizeSpeaker.bio),
                        Language.FRENCH to markdownValidator.sanitize(sessionizeSpeaker.bio)
                    ),
                    links = sessionizeSpeaker.links.map { link ->
                        Link(link.title, link.url)
                    },
                    photoUrl = sessionizeSpeaker.picture,
                    emailHash = sessionizeSpeaker.email?.encodeToMd5(),
                    company = sessionizeSpeaker.tagLine,
                    cfpId = sessionizeSpeaker.id
                )
            }


        return existingSpeakers + nonExistingSpeakers
    }
}
