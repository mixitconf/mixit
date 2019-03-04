package mixit.import

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import mixit.model.*
import mixit.repository.UserRepository
import mixit.util.Cryptographer
import mixit.util.encodeToMd5
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL


inline fun <reified T> ObjectMapper.readValue(src: InputStream): T = readValue(src, jacksonTypeRef<T>())

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Disabled
class SessionizeImportTests(@Autowired val objectMapper: ObjectMapper,
                            @Autowired val userRepository: UserRepository,
                            @Autowired val cryptographer: Cryptographer) {


    val speakerWithPngImage = listOf<String>("chabanois", "engel", "galson", "gilet", "grangeau", "jakobs", "mccullagh00", "paccard", "poppendieck", "stormacq", "topçu", "vuillard")

    @Test
    fun `load speakers`() {
        val speakersToPersist = mutableListOf<User>()
        val sessionsToPersist = mutableListOf<Talk>()

        val speakerIdsResource = ClassPathResource("import/speaker_ids.json")
        val speakerIds: List<SessionizeSpeakerId> = objectMapper.readValue(speakerIdsResource.inputStream)

        val speakersResource = ClassPathResource("import/speakers.json")
        val speakers: List<SessionizeSpeaker> = objectMapper.readValue(speakersResource.inputStream)

        initializeFolder()

        speakerIds.forEach { speakerId ->
            val speakerWithInfo = speakers.first { s ->
                s.id == speakerId.id
            }
            val imageName = sanitize(speakerWithInfo.lastName)
            val filenameImage = imageName + if (speakerWithPngImage.any { it == imageName }) ".png" else ".jpg"

            // 1. we find user in our database
            val user = userRepository.findByEmail(speakerId.email).block()
            if (user != null) {
                println("User found => ${speakerId.id} : ${speakerId.lastName} ${speakerId.firstName}")

                val imageUrl = getImageUrl(user, speakerWithInfo)
                if (imageUrl != null && imageUrl.contains("sessionize")) {
                    downloadImage(imageUrl, filenameImage)
                }
                speakersToPersist.add(
                        User(user.login,
                                speakerId.firstName,
                                speakerId.lastName,
                                user.email,
                                if (speakerWithInfo.company == null) user.company else speakerWithInfo.company,
                                if (speakerWithInfo.company == null) user.description else bioToMap(speakerWithInfo.bio),
                                speakerId.email.encodeToMd5(),
                                "https://mixitconf.cleverapps.io/images/speakers/${filenameImage}",
                                user.role,
                                speakerWithInfo.findSpeakerLinks(),
                                // We store id in token to retrieve speaker later when we will save talk
                                token = speakerId.id))

                println("User updated")

            } else {
                var login = speakerId.email.substring(0, speakerId.email.indexOf("@"))
                val userCheck = userRepository.findOne(login).block()
                if(userCheck !=null){
                    println("Login already exist")
                    login = login + speakerId.lastName
                    val userCheck = userRepository.findOne(login).block()
                    if(userCheck !=null){
                        println("Login already exist")
                        throw IllegalArgumentException()
                    }
                }
                println("User not found => ${speakerId.id} : ${speakerId.lastName} ${speakerId.firstName} => login : $login")

                if (speakerWithInfo.profilePicture != null) {
                    downloadImage(speakerWithInfo.profilePicture, filenameImage)
                }
                speakersToPersist.add(
                        User(login,
                                speakerId.firstName,
                                speakerId.lastName,
                                cryptographer.encrypt(speakerId.email),
                                speakerWithInfo.company,
                                bioToMap(speakerWithInfo.bio),
                                speakerId.email.encodeToMd5(),
                                "https://mixitconf.cleverapps.io/images/speakers/${filenameImage}",
                                Role.USER,
                                speakerWithInfo.findSpeakerLinks(),
                                // We store id in token to retrieve speaker later when we will save talk
                                token = speakerId.id))

                println("User created")
            }
        }

        // Now we have a list of speakers and we write this list in a file
        objectMapper.writeValue(File("/tmp/mixit/speakers_2019.json"), speakersToPersist)

        // In the second step we have to create the sessions

        val sessionResource = ClassPathResource("import/talks.json")
        val sessions: List<SessionizeTalk> = objectMapper.readValue(sessionResource.inputStream)

        sessions.forEach { session ->
            // We have to find the sessions speakers
            val sessionSpeakers: List<User> = session.speakers.map { sp -> speakersToPersist.first { it.token == sp } }

            val language = session.categoryItems?.filter { elt -> language.any { it.first == elt } }?.first()
            val topic = session.categoryItems?.filter { elt -> tags.any { it.first == elt } }?.first()
            val format = session.categoryItems?.filter { elt -> categories.any { it.first == elt } }?.first()

            println("Session ${session.title} : speakers ${sessionSpeakers.map { it.lastname }}")
            println("Lng=${if(language == null) Language.FRENCH else toTalkLanguage(language) } " +
                    "topic=${if(topic == null) "other" else toTopic(topic) } " +
                    "format=${if(format == null) TalkFormat.TALK else toTalkFormat(format)}")

            sessionsToPersist.add(Talk(
                    if (format == null) TalkFormat.TALK else toTalkFormat(format),
                    "2019",
                    session.title,
                    session.description,
                    sessionSpeakers.map { it.login },
                    if (language == null) Language.FRENCH else toTalkLanguage(language),
                    topic = if (topic == null) "other" else toTopic(topic)
            ))
        }

        objectMapper.writeValue(File("/tmp/mixit/talks_2019.json"), sessionsToPersist)

    }
}

fun getImageUrl(user: User, userSessionIze: SessionizeSpeaker): String? {
    System.out.println("user ${user.lastname} -> ${user.photoUrl}")
    if (!user.photoUrl.isNullOrEmpty()) {
        if (user.photoUrl!!.contains("/mixitconf")) {
            return user.photoUrl
        }
        if (user.photoUrl!!.startsWith("/images")) {
            return "https://mixitconf.cleverapps.io${user.photoUrl}"
        }
        return user.photoUrl
    } else if (userSessionIze.profilePicture.isNullOrEmpty()) {
        return "https://www.gravatar.com/avatar/${user.emailHash}?s=400"
    }
    return userSessionIze.profilePicture
}


fun bioToMap(bio: String?) = if (bio == null) mapOf(Pair(Language.FRENCH, "UNKNOWN"), Pair(Language.ENGLISH, "UNKNOWN")) else mapOf(Pair(Language.FRENCH, bio), Pair(Language.ENGLISH, bio))

val SPECIAL_SLUG_CHARACTERS = mapOf(Pair('é', 'e'), Pair('è', 'e'), Pair('ï', 'i'), Pair(' ', '_'), Pair('ê', 'e'), Pair('à', 'a'), Pair('-', '_'), Pair('_', '0'), Pair('∴', '0'))

fun sanitize(value: String): String = value.toLowerCase().toCharArray().map { if (SPECIAL_SLUG_CHARACTERS.get(it) == null) it else SPECIAL_SLUG_CHARACTERS.get(it) }.joinToString("")

fun initializeFolder() {
    // Check if /tmp/images/ exists ?
    val directory = File("/tmp/mixit/")
    if (!directory.exists()) {
        directory.mkdir()
    }
}

fun downloadImage(url: String, filename: String) {
    val emplacement = File("/tmp/mixit/${filename}")
    if (emplacement.exists()) {
        emplacement.delete()
    }
    emplacement.createNewFile()

    FileOutputStream(emplacement).use {
        val out = it

        try {
            URL(url).openConnection().getInputStream().use {
                val b = ByteArray(1024)
                var c: Int = it.read(b)
                while (c != -1) {
                    out.write(b, 0, c)
                    c = it.read(b)
                }
            }
        } catch (e: Exception) {
            System.out.println("Impossible to load image for ${filename}")
            e.printStackTrace()
        }
    }
}

open class SessionizeSpeakerId(
        val id: String,
        val firstName: String,
        val lastName: String,
        val email: String
)

class SessionizeTalk(
        val id: String,
        val title: String,
        val description: String,
        val speakers: List<String>,
        val categoryItems: List<Int>? = null,
        val startsAt: String? = null,
        val endsAt: String? = null,
        val isServiceSession: Boolean? = false,
        val isPlenumSession: Boolean? = false,
        val questionAnswers: List<String>? = null,
        val roomId: String? = null
)

class SessionizeSpeaker(
        val id: String,
        val firstName: String,
        val lastName: String,
        val fullName: String? = null,
        val company: String? = null,
        val bio: String? = null,
        val tagLine: String? = null,
        val profilePicture: String? = null,
        val links: List<SessionizeLink>? = null
)

fun SessionizeSpeaker.findSpeakerLinks(): List<Link> = if (links == null) emptyList() else
    links.map { Link(it.title, it.url) }


class SessionizeLink(
        val title: String,
        val url: String,
        val linkType: String
)

val categories = arrayOf(
        Pair(13709, "Session"),
        Pair(13710, "Workshop"),
        Pair(20067, "Surprise keynote"),
        Pair(20068, "Random")
)

fun toTalkFormat(elt: Int): TalkFormat =
        when (categories.first { it.first == elt }.second) {
            "Session" -> TalkFormat.TALK
            "Workshop" -> TalkFormat.WORKSHOP
            "Surprise keynote" -> TalkFormat.KEYNOTE_SURPRISE
            "Random" -> TalkFormat.RANDOM
            else -> TalkFormat.TALK
        }

val items = arrayOf(
        Pair(13714, "Introductory and overview"),
        Pair(13716, "Advanced")
)

val language = arrayOf(
        Pair(13718, "English"),
        Pair(13722, "French")
)

fun toTalkLanguage(elt: Int): Language =
        when (language.first { it.first == elt }.second) {
            "English" -> Language.ENGLISH
            else -> Language.FRENCH
        }


val tags = arrayOf(
        Pair(15603, "Life Style"),
        Pair(15604, "Ethic"),
        Pair(15605, "Team"),
        Pair(15606, "Aliens"),
        Pair(15607, "Tech"),
        Pair(15608, "Maker"),
        Pair(15609, "Design")
)

fun toTopic(elt: Int): String =
        when (tags.first { it.first == elt }.second) {
            "Maker" -> "makers"
            "Design" -> "design"
            "Aliens" -> "aliens"
            "Tech" -> "tech"
            "Ethic" -> "ethics"
            "Life Style" -> "lifestyle"
            "Team" -> "team"
            else -> "other"
        }
