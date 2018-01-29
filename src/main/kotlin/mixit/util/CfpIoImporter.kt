package mixit.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.*
import mixit.repository.TalkRepository
import mixit.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Class used to generate session from a cfp.io export (http://mix-it.cfp.io/api/proposals). Sessions are exported from http://mix-it.cfp.io/api/proposals
 * This class will be deleted when everything will be imported
 */
@Component
class CfpIoImporter(private val userReposittory: UserRepository,
                    private val talkRepository: TalkRepository,
                    private val objectMapper: ObjectMapper) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun initData() {
        if (talkRepository.findByEvent("2018").count().block() == 0L) {
            logger.info("Cfp io import starts")

            val eventsResource = ClassPathResource("data/cfp/sessions.json")
            val cfpIoTalks: List<CfpioTalk> = objectMapper.readValue(eventsResource.inputStream)

            cfpIoTalks
                    .filter { it.state == "ACCEPTED" }
                    .map{
                        val logins = mutableListOf(saveSpeaker(it.speaker).login)
                        it.cospeakers?.map { saveSpeaker(it).login }?.forEach { logins.add(it) }
                        it.toTalk(logins)
                    }
                    .forEach { talkRepository.save(it).block()}

            logger.info("Cfp io data are initialized")
        }
    }

    private fun saveRandomsForLaterUpdate(cfpIoTalks: List<CfpioTalk>) {
        cfpIoTalks.filter { it.state == "ACCEPTED" }
                .map{
                    val logins = mutableListOf(saveSpeaker(it.speaker).login)
                    it.cospeakers?.map { saveSpeaker(it).login }?.forEach { logins.add(it) }
                    it.toTalk(logins)
                }
                .forEach { talkRepository.save(it).block()}
    }

    private fun saveTalks(cfpIoTalks: List<CfpioTalk>) {
        cfpIoTalks.filter { it.state == "ACCEPTED" }
                .map{
                    val logins = mutableListOf(saveSpeaker(it.speaker).login)
                    it.cospeakers?.map { saveSpeaker(it).login }?.forEach { logins.add(it) }
                    it.toTalk(logins)
                }
                .forEach { talkRepository.save(it).block()}
    }

    private fun saveSpeaker(speaker: CfpioSpeaker): User {
        val user = userReposittory.findByEmail(speaker.email).block()
        return userReposittory.save(if (user == null) speaker.toUser() else speaker.toUser(user)).block()!!
    }
}

private val formats = mapOf(
        Pair(43, TalkFormat.TALK),
        Pair(48, TalkFormat.RANDOM),
        Pair(49, TalkFormat.WORKSHOP),
        Pair(50, TalkFormat.KEYNOTE)
)

private val languages = mapOf(
        Pair("Français", Language.FRENCH),
        Pair("English", Language.ENGLISH)
)

private val topics = mapOf(
        Pair("Team", "team"),
        Pair("Alien", "aliens"),
        Pair("Hack/Activism", "hacktivism"),
        Pair("Design", "design"),
        Pair("Tech", "tech"),
        Pair("Other", "learn"),
        Pair("other", "learn"),
        Pair("Maker", "makers"),
        Pair("Product", "makers"),
        Pair("Education", "learn")
)

private data class CfpioTalk(
        val id: String,
        val state: String,
        val name: String,
        val language: String?,
        val description: String,
        val plus: String?,
        val eventId: String,
        val format: Int,
        val trackLabel: String,
        val difficulty: Int,
        val speaker: CfpioSpeaker,
        val cospeakers: Array<CfpioSpeaker>?
) {

    fun toTalk(speakers: List<String>) = Talk(
            formats.getOrDefault(format, TalkFormat.TALK),
            "2018",
            name,
            description,
            speakers,
            languages.getOrDefault(language, Language.FRENCH),
            LocalDateTime.now(),
            if (plus == null) "" else plus,
            topics.getOrDefault(trackLabel, "Other"),
            start = LocalDateTime.of(2018, 4, 19, 8, 0, 0),
            end = LocalDateTime.of(2018, 4, 19, 8, 50, 0),
            room = Room.UNKNOWN
    )
}

private data class CfpioSpeaker(
        val id: String,
        val email: String,
        val lastname: String?,
        val firstname: String?,
        val bio: String?,
        val company: String?,
        val twitter: String?,
        val googleplus: String?,
        val github: String?,
        val language: String?,
        val gender: String,
        val social: String?,
        val imageProfilURL: String?
) {
    fun toUser(user: User) = User(
            user.login,
            user.firstname,
            user.lastname,
            email,
            company,
            if (bio.isNullOrEmpty()) user.description
            else mapOf(Pair(Language.FRENCH, bio!!), Pair(Language.ENGLISH, bio)),
            if (imageProfilURL.isNullOrEmpty()) user.emailHash else null,
            if (imageProfilURL.isNullOrEmpty()) user.photoUrl else imageProfilURL,
            user.role,
            if (findSpeakerLinks().isEmpty()) user.links else findSpeakerLinks()
    )

    fun toUser() = User(
            email,
            if (firstname.isNullOrEmpty()) "UNKNOWN" else firstname!!,
            if (lastname.isNullOrEmpty()) "UNKNOWN" else lastname!!,
            email,
            company,
            if (bio.isNullOrEmpty()) mapOf(Pair(Language.FRENCH, "UNKNOWN"), Pair(Language.ENGLISH, "UNKNOWN")) else mapOf(Pair(Language.FRENCH, bio!!), Pair(Language.ENGLISH, bio)),
            if (imageProfilURL.isNullOrEmpty()) email.encodeToMd5() else null,
            imageProfilURL,
            Role.USER,
            findSpeakerLinks()
    )

    private fun findSpeakerLinks(): List<Link> {
        val list = mutableListOf<Link>()
        if (!github.isNullOrEmpty()) {
            list.add(Link("Github", github!!))
        }
        if (!twitter.isNullOrEmpty()) {
            list.add(Link("Twitter", twitter!!))
        }
        if (!googleplus.isNullOrEmpty()) {
            list.add(Link("Google+", googleplus!!))
        }
        if (!social.isNullOrEmpty()) {
            list.add(Link("Autre", social!!))
        }
        return list
    }
}