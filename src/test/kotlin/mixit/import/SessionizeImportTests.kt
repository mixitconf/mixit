package mixit.import

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import mixit.model.*
import mixit.repository.UserRepository
import mixit.util.Cryptographer
import mixit.util.encodeToMd5
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.InputStream
import java.util.*


inline fun <reified T> ObjectMapper.readValue(src: InputStream): T = readValue(src, jacksonTypeRef<T>())

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionizeImportTests(@Autowired val objectMapper: ObjectMapper,
                            @Autowired val userRepository: UserRepository,
                            @Autowired val cryptographer: Cryptographer) {


    val speakerWithPngImage = listOf("julien_dubedout", "marilyn_kol", "benoit", "cedric_spalvieri", "nikola_lohinski")

    @Test
    fun `load speakers`() {

        initializeFolder()

        val papercallExports = objectMapper.readValue<List<PapercallExport>>(ClassPathResource("import/2020-papercall-export.json").inputStream)

        val speakersToPersist = importSpeakers(papercallExports)
        objectMapper.writeValue(File("/tmp/mixit/speakers_2019.json"), speakersToPersist)

        val sessionsToPersist = createSessions(papercallExports, speakersToPersist)
        objectMapper.writeValue(File("/tmp/mixit/talks_2019.json"), sessionsToPersist)

    }

    private fun importSpeakers(papercallExports: List<PapercallExport>): MutableList<User> {

        val papercallSpeakers = extractSpeakers(papercallExports)

        val speakersToPersist:MutableList<User> = mutableListOf()

        papercallSpeakers.forEach { papercallSpeaker ->
            val user = userRepository.findByNonEncryptedEmail(papercallSpeaker.email).block()
            if (user != null) {
                val updatedUser = updateUser(user, papercallSpeaker)
                speakersToPersist.add(updatedUser)
            } else {
                val createdUser = createUser(papercallSpeaker)
                speakersToPersist.add(createdUser)
            }
        }

        return speakersToPersist
    }

    private fun getImageName(speakerName: String): String {
        val imageName = sanitize(speakerName)
        return imageName + if (speakerWithPngImage.any { it == imageName }) ".png" else ".jpg"
    }

    private fun extractSpeakers(papercallExports: List<PapercallExport>): MutableList<PapercallProfile> {
        val speakers = mutableListOf<PapercallProfile>()

        //Add a reference to the talk in the user to be able to link them
        papercallExports.forEach { talk ->
            talk.profile.talkId = talk.id
            talk.co_presenter_profiles.forEach { coPresenter -> coPresenter.talkId = talk.id }
        }

        papercallExports
                .map { papercallExport -> papercallExport.profile }
                .forEach { speakers.add(it) }

        papercallExports
                .flatMap { papercallExport -> papercallExport.co_presenter_profiles }
                .forEach { speakers.add(it) }

        return speakers
    }

    private fun createUser(papercallSpeaker: PapercallProfile): User {
        var login = papercallSpeaker.email.substring(0, papercallSpeaker.email.indexOf("@"))
        val userCheck = userRepository.findOne(login).block()
        if (userCheck != null) {
            println("Login ${userCheck.login} already exist: try to create one with suffix")
            login += UUID.randomUUID().toString()
            val anotherUserCheck = userRepository.findOne(login).block()
            if (anotherUserCheck != null) {
                println("Login with suffix ${anotherUserCheck.login} already exist aborting import")
                throw IllegalArgumentException()
            }
        }
        println("User not found => ${papercallSpeaker.name} => login : $login")

        if (papercallSpeaker.avatar != null) {
            downloadImage(papercallSpeaker.avatar, getImageName(papercallSpeaker.name))
        }
        println("User created")

        return User(login,
                firstNameOf(papercallSpeaker.name),
                lastName(papercallSpeaker.name),
                cryptographer.encrypt(papercallSpeaker.email),
                papercallSpeaker.company,
                bioToMap(papercallSpeaker.bio),
                papercallSpeaker.email.encodeToMd5(),
                "https://mixitconf.cleverapps.io/images/speakers/${getImageName(papercallSpeaker.name)}",
                Role.USER,
                if (papercallSpeaker.url.isNullOrEmpty()) listOf() else listOf(Link("site", papercallSpeaker.url)),
                // We store id in token to retrieve speaker later when we will save talk
                token = papercallSpeaker.talkId.orEmpty()
        )

    }

    private fun updateUser(user: User, papercallSpeaker: PapercallProfile): User {
        println("User found => ${user.login} : ${user.lastname} ${user.firstname}")

        val imageUrl = getImageUrl(user, papercallSpeaker.avatar)
        if (imageUrl != null && imageUrl.contains("papercallio")) {
            downloadImage(imageUrl, getImageName(papercallSpeaker.name))
        }
        println("User updated")

        return User(user.login,
                        firstNameOf(papercallSpeaker.name),
                        lastName(papercallSpeaker.name),
                        user.email,
                        if (papercallSpeaker.company == null) user.company else papercallSpeaker.company,
                        if (papercallSpeaker.bio == null) user.description else bioToMap(papercallSpeaker.bio),
                        papercallSpeaker.email.encodeToMd5(),
                        "https://mixitconf.cleverapps.io/images/speakers/${getImageName(papercallSpeaker.name)}",
                        user.role,
                        if (papercallSpeaker.url.isNullOrEmpty()) listOf() else listOf(Link("site", papercallSpeaker.url)),
                        // We store id in token to retrieve speaker later when we will save talk
                        token = papercallSpeaker.talkId.orEmpty()
        )
    }

    private fun lastName(name: String) = name.split(" ").drop(1).joinToString(" ")

    private fun firstNameOf(name: String) = name.split(" ").first()

    private fun createSessions(papercallExports: List<PapercallExport>, speakersToPersist: MutableList<User>): MutableList<Talk> {
        val sessionsToPersist = mutableListOf<Talk>()
        val sessions: List<SessionizeTalk> = objectMapper.readValue(ClassPathResource("import/talks.json").inputStream)

        sessions.forEach { session ->
            // We have to find the sessions speakers
            val sessionSpeakers: List<User> = session.speakers.map { sp -> speakersToPersist.first { it.token == sp } }

            val language = session.categoryItems?.filter { elt -> language.any { it.first == elt } }?.first()
            val topic = session.categoryItems?.filter { elt -> tags.any { it.first == elt } }?.first()
            val format = session.categoryItems?.filter { elt -> categories.any { it.first == elt } }?.first()

            println("Session ${session.title} : speakers ${sessionSpeakers.map { it.lastname }}")
            println("Lng=${if (language == null) Language.FRENCH else toTalkLanguage(language)} " +
                    "topic=${if (topic == null) "other" else toTopic(topic)} " +
                    "format=${if (format == null) TalkFormat.TALK else toTalkFormat(format)}")

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

        return sessionsToPersist
    }
}

fun getImageUrl(user: User, pictureUrl: String?): String? {
    System.out.println("user ${user.lastname} -> ${user.photoUrl}")
    if (!user.photoUrl.isNullOrEmpty()) {
        if (user.photoUrl!!.contains("/mixitconf")) {
            return user.photoUrl
        }
        if (user.photoUrl!!.startsWith("/images")) {
            return "https://mixitconf.cleverapps.io${user.photoUrl}"
        }
        return user.photoUrl
    } else if (pictureUrl.isNullOrEmpty()) {
        return "https://www.gravatar.com/avatar/${user.emailHash}?s=400"
    }
    return pictureUrl
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
    /*val emplacement = File("/tmp/mixit/$filename")
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
    }*/
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

class PapercallExport(
        val id: String? = null,
        val state: String? = null,
        val talk: PapercallTalk? = null,
        val profile: PapercallProfile,
        val co_presenter_profiles: List<PapercallProfile> = emptyList()
)

class PapercallProfile(
        val name: String,
        val bio: String? = null,
        val twitter: String? = null,
        val company: String? = null,
        val url: String? = null,
        val email: String,
        val avatar: String? = null,
        var talkId: String? = null
)

class PapercallTalk(
        val title: String? = null,
        val abstract: String? = null,
        val description: String? = null,
        val talk_format: String? = null
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
